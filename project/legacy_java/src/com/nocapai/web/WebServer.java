package com.nocapai.web;

import com.nocapai.model.Book;
import com.nocapai.model.Member;
import com.nocapai.model.Transaction;
import com.nocapai.service.LibraryService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Dependency-free HTTP API + static frontend for NoCapAI. */
public class WebServer {
    private static final LibraryService service = new LibraryService();
    private static final Path STATIC = Paths.get("web").toAbsolutePath().normalize();
    private static final Path DATA = Paths.get(System.getenv().getOrDefault("DATA_DIR", "data"), "library.state").toAbsolutePath().normalize();
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private record Session(String role, String memberId, long expiresAt) {}

    public static void main(String[] args) throws Exception {
        try { service.loadState(DATA); } catch (Exception e) { System.err.println("State load skipped: " + e.getMessage()); }
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api/", WebServer::api);
        server.createContext("/", WebServer::staticFile);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(12));
        Runtime.getRuntime().addShutdownHook(new Thread(WebServer::persist));
        System.out.println("No Cap AI online server listening on " + port);
        server.start();
    }

    private static void api(HttpExchange ex) throws IOException {
        headers(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); ex.close(); return; }
        String path = ex.getRequestURI().getPath().substring("/api".length());
        try {
            if (path.equals("/health")) { send(ex,200,"{\"ok\":true,\"service\":\"No Cap AI\"}"); return; }
            if (path.equals("/login") && "POST".equals(ex.getRequestMethod())) { login(ex); return; }
            if (path.equals("/logout") && "POST".equals(ex.getRequestMethod())) { sessions.remove(token(ex)); send(ex,200,"{\"ok\":true}"); return; }
            Session s = requireAuth(ex); if (s == null) return;
            if (path.equals("/me") && "GET".equals(ex.getRequestMethod())) { send(ex,200,"{\"role\":"+q(s.role())+",\"memberId\":"+(s.memberId()==null?"null":q(s.memberId()))+"}"); return; }
            if (path.equals("/books") && "GET".equals(ex.getRequestMethod())) { books(ex); return; }
            if (path.equals("/books") && "POST".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) addBook(ex); return; }
            if (path.startsWith("/books/") && "PUT".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) editBook(ex,path.substring(7)); return; }
            if (path.startsWith("/books/") && "DELETE".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) sendMsg(ex,service.deleteBook(path.substring(7))); return; }
            if (path.equals("/members") && "GET".equals(ex.getRequestMethod())) { members(ex,s); return; }
            if (path.equals("/members") && "POST".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) addMember(ex); return; }
            if (path.startsWith("/members/") && "PUT".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) editMember(ex,path.substring(9)); return; }
            if (path.startsWith("/members/") && "DELETE".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) sendMsg(ex,service.deleteMember(path.substring(9))); return; }
            if (path.equals("/issue") && "POST".equals(ex.getRequestMethod())) { issue(ex,s); return; }
            if (path.equals("/return") && "POST".equals(ex.getRequestMethod())) { ret(ex,s); return; }
            if (path.equals("/actions") && "GET".equals(ex.getRequestMethod())) { adminOnly(s,ex); if(!ex.getResponseHeaders().containsKey("X-Auth-Error")) actions(ex); return; }
            if (path.equals("/reports") && "GET".equals(ex.getRequestMethod())) { reports(ex); return; }
            if (path.equals("/queues") && "GET".equals(ex.getRequestMethod())) { queues(ex); return; }
            send(ex,404,"{\"error\":\"Endpoint not found\"}");
        } catch (IllegalArgumentException e) { send(ex,400,"{\"error\":"+q(e.getMessage())+"}"); }
          catch (Exception e) { e.printStackTrace(); send(ex,500,"{\"error\":\"Internal server error\"}"); }
    }

    private static void login(HttpExchange ex) throws IOException {
        Map<String,String> b=parseBody(read(ex)); String role=b.getOrDefault("role","").trim(); String username=b.getOrDefault("username","").trim(); String password=b.getOrDefault("password","");
        String memberId=null, name="";
        if (role.equals("Librarian") && username.equals("admin") && password.equals("admin123")) { }
        else if (role.equals("Student") && password.equals("student123")) { Member m=service.findMemberById(username); if(m==null){send(ex,401,"{\"error\":\"Member ID not found\"}");return;} memberId=m.getId(); name=m.getName(); }
        else { send(ex,401,"{\"error\":\"Invalid credentials\"}"); return; }
        String token=UUID.randomUUID().toString(); sessions.put(token,new Session(role,memberId,System.currentTimeMillis()+8*60*60*1000L));
        send(ex,200,"{\"ok\":true,\"token\":"+q(token)+",\"role\":"+q(role)+",\"memberId\":"+(memberId==null?"null":q(memberId))+",\"name\":"+q(name)+"}");
    }

    private static Session requireAuth(HttpExchange ex) throws IOException { String t=token(ex); Session s=sessions.get(t); if(s==null||s.expiresAt()<System.currentTimeMillis()){sessions.remove(t);send(ex,401,"{\"error\":\"Session expired. Please log in again.\"}");return null;}return s; }
    private static void adminOnly(Session s,HttpExchange ex)throws IOException{if(!"Librarian".equals(s.role())){ex.getResponseHeaders().set("X-Auth-Error","1");send(ex,403,"{\"error\":\"Librarian access required\"}");}}
    private static String token(HttpExchange ex){String h=ex.getRequestHeaders().getFirst("Authorization");return h!=null&&h.startsWith("Bearer ")?h.substring(7):"";}

    private static void books(HttpExchange ex)throws IOException{String query=Optional.ofNullable(ex.getRequestURI().getQuery()).orElse("");String search=param(query,"search");List<Book> list=search.isBlank()?service.getBooksSortedByTitle():service.searchBooks(search);send(ex,200,"["+list.stream().map(WebServer::bookJson).collect(Collectors.joining(","))+"]");}
    private static void addBook(HttpExchange ex)throws IOException{Map<String,String>b=parseBody(read(ex));String id=req(b,"id"),title=req(b,"title"),author=req(b,"author"),genre=req(b,"genre");int copies=positiveInt(b.getOrDefault("copies","1"));sendMsg(ex,service.addBook(id,title,author,genre,copies));persist();}
    private static void editBook(HttpExchange ex,String id)throws IOException{Map<String,String>b=parseBody(read(ex));String title=req(b,"title"),author=req(b,"author"),genre=req(b,"genre");int copies=positiveInt(b.getOrDefault("copies","1"));sendMsg(ex,service.editBook(id,title,author,genre,copies));persist();}
    private static void addMember(HttpExchange ex)throws IOException{Map<String,String>b=parseBody(read(ex));sendMsg(ex,service.addMember(req(b,"id"),req(b,"name"),req(b,"contact")));persist();}
    private static void editMember(HttpExchange ex,String id)throws IOException{Map<String,String>b=parseBody(read(ex));sendMsg(ex,service.editMember(id,req(b,"name"),req(b,"contact")));persist();}
    private static void members(HttpExchange ex,Session s)throws IOException{List<Member> list=service.getAllMembers();if("Student".equals(s.role()))list=list.stream().filter(m->m.getId().equals(s.memberId())).toList();send(ex,200,"["+list.stream().map(WebServer::memberJson).collect(Collectors.joining(","))+"]");}
    private static void issue(HttpExchange ex,Session s)throws IOException{Map<String,String>b=parseBody(read(ex));String memberId=b.getOrDefault("memberId","");if("Student".equals(s.role()))memberId=s.memberId();String msg=service.issueBook(req(b,"bookId"),memberId);sendMsg(ex,msg);persist();}
    private static void ret(HttpExchange ex,Session s)throws IOException{Map<String,String>b=parseBody(read(ex));String memberId=b.getOrDefault("memberId","");if("Student".equals(s.role()))memberId=s.memberId();String msg=service.returnBook(req(b,"bookId"),memberId);sendMsg(ex,msg);persist();}
    private static void actions(HttpExchange ex)throws IOException{send(ex,200,"["+service.getRecentActions().stream().map(WebServer::txJson).collect(Collectors.joining(","))+"]");}
    private static void queues(HttpExchange ex)throws IOException{List<String>ids=service.getBooksWithActiveQueues();String out=ids.stream().map(id->{var q=service.getQueueForBook(id);String names=q.toList().stream().map(m->q(m.getName())).collect(Collectors.joining(","));Book b=service.findBookById(id);return "{\"bookId\":"+q(id)+",\"bookTitle\":"+q(b==null?id:b.getTitle())+",\"members\":["+names+"]}";}).collect(Collectors.joining(","));send(ex,200,"["+out+"]");}
    private static void reports(HttpExchange ex)throws IOException{var issued=service.getCurrentlyIssued();var overdue=service.getOverdueBooks();var popular=service.getMostPopularBooks();String i=issued.stream().map(r->"{\"book\":"+bookJson(r.book)+",\"member\":"+memberJson(r.member)+",\"dueDate\":"+q(r.dueDate.toString())+"}").collect(Collectors.joining(","));String o=overdue.stream().map(r->"{\"book\":"+bookJson(r.book)+",\"member\":"+memberJson(r.member)+",\"dueDate\":"+q(r.dueDate.toString())+"}").collect(Collectors.joining(","));String p=popular.stream().map(e->"{\"book\":"+bookJson(e.getKey())+",\"count\":"+e.getValue()+"}").collect(Collectors.joining(","));send(ex,200,"{\"issued\":["+i+"],\"overdue\":["+o+"],\"popular\":["+p+"]}");}

    private static String req(Map<String,String>b,String k){String v=b.get(k);if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(k+" is required");return v.trim();}
    private static int positiveInt(String s){try{int n=Integer.parseInt(s);if(n<1||n>100000)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("copies must be a positive number");}}
    private static String bookJson(Book b){return "{\"id\":"+q(b.getId())+",\"title\":"+q(b.getTitle())+",\"author\":"+q(b.getAuthor())+",\"genre\":"+q(b.getGenre())+",\"totalCopies\":"+b.getTotalCopies()+",\"availableCopies\":"+b.getAvailableCopies()+"}";}
    private static String memberJson(Member m){return "{\"id\":"+q(m.getId())+",\"name\":"+q(m.getName())+",\"contact\":"+q(m.getContact())+",\"booksHeld\":"+m.getIssuedDueDates().size()+"}";}
    private static String txJson(Transaction t){return "{\"type\":"+q(t.getType().name())+",\"bookId\":"+q(t.getBookId())+",\"bookTitle\":"+q(t.getBookTitle())+",\"memberId\":"+q(t.getMemberId())+",\"memberName\":"+q(t.getMemberName())+",\"date\":"+q(t.getDate().toString())+",\"dueDate\":"+(t.getDueDate()==null?"null":q(t.getDueDate().toString()))+",\"overdue\":"+t.isWasOverdue()+"}";}
    private static void sendMsg(HttpExchange ex,String msg)throws IOException{send(ex,200,"{\"message\":"+q(msg)+"}");}
    private static String q(String s){if(s==null)return "null";return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
    private static void send(HttpExchange ex,int status,String body)throws IOException{byte[]d=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(status,d.length);try(OutputStream o=ex.getResponseBody()){o.write(d);}}
    private static void headers(HttpExchange ex){Headers h=ex.getResponseHeaders();h.set("Access-Control-Allow-Origin","*");h.set("Access-Control-Allow-Headers","Content-Type, Authorization");h.set("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");h.set("Cache-Control","no-store");}
    private static String read(HttpExchange ex)throws IOException{return new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
    private static String param(String qs,String key){for(String p:qs.split("&")){String[]a=p.split("=",2);if(a.length==2&&a[0].equals(key))return URLDecoder.decode(a[1],StandardCharsets.UTF_8);}return "";}
    private static Map<String,String> parseBody(String s){Map<String,String>m=new HashMap<>();java.util.regex.Matcher x=java.util.regex.Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|([^,}]+))").matcher(s);while(x.find()){String v=x.group(2)!=null?x.group(2).replace("\\\"","\"").replace("\\\\","\\"):x.group(3).trim();m.put(x.group(1),v);}return m;}
    private static void staticFile(HttpExchange ex)throws IOException{headers(ex);String p=ex.getRequestURI().getPath();if(p.equals("/"))p="/index.html";Path f=STATIC.resolve(p.substring(1)).normalize();if(!f.startsWith(STATIC)||!Files.exists(f)||Files.isDirectory(f)){sendText(ex,404,"Not found");return;}String ct=p.endsWith(".css")?"text/css":p.endsWith(".js")?"application/javascript":p.endsWith(".svg")?"image/svg+xml":"text/html";byte[]d=Files.readAllBytes(f);ex.getResponseHeaders().set("Content-Type",ct+"; charset=utf-8");ex.sendResponseHeaders(200,d.length);try(OutputStream o=ex.getResponseBody()){o.write(d);}}
    private static void sendText(HttpExchange ex,int status,String body)throws IOException{byte[]d=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","text/plain; charset=utf-8");ex.sendResponseHeaders(status,d.length);try(OutputStream o=ex.getResponseBody()){o.write(d);}}
    private static synchronized void persist(){try{service.saveState(DATA);}catch(Exception e){System.err.println("State save failed: "+e.getMessage());}}
}
