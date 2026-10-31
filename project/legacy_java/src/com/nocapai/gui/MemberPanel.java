package com.nocapai.gui;

import com.nocapai.model.Member;
import com.nocapai.service.LibraryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Member Management module — add, edit, delete, and view members. */
public class MemberPanel extends JPanel {

    private final LibraryService service;
    private DefaultTableModel tableModel;
    private JTable table;

    public MemberPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        buildUI();
        refreshTable();
    }

    private void buildUI() {
        String[] columns = {"ID", "Name", "Contact", "Books Currently Held"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton addButton = new JButton("Add Member");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());
        refreshButton.addActionListener(e -> refreshTable());

        bottom.add(addButton);
        bottom.add(editButton);
        bottom.add(deleteButton);
        bottom.add(refreshButton);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Member> members = service.getAllMembers();
        for (Member m : members) {
            tableModel.addRow(new Object[]{
                    m.getId(), m.getName(), m.getContact(), m.getIssuedDueDates().size()
            });
        }
    }

    private String getSelectedMemberId() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a member from the table first.",
                    "No member selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) tableModel.getValueAt(row, 0);
    }

    private void showAddDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();

        JPanel form = buildForm(
                new String[]{"Member ID:", "Name:", "Contact:"},
                new JComponent[]{idField, nameField, contactField});

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Member",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String message = service.addMember(idField.getText().trim(), nameField.getText().trim(),
                    contactField.getText().trim());
            JOptionPane.showMessageDialog(this, message + "\n\n(Default student login password: student123)");
            refreshTable();
        }
    }

    private void showEditDialog() {
        String id = getSelectedMemberId();
        if (id == null) return;
        Member member = service.findMemberById(id);
        if (member == null) return;

        JTextField nameField = new JTextField(member.getName());
        JTextField contactField = new JTextField(member.getContact());

        JPanel form = buildForm(
                new String[]{"Name:", "Contact:"},
                new JComponent[]{nameField, contactField});

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Member: " + id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String message = service.editMember(id, nameField.getText().trim(), contactField.getText().trim());
            JOptionPane.showMessageDialog(this, message);
            refreshTable();
        }
    }

    private void deleteSelected() {
        String id = getSelectedMemberId();
        if (id == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete member " + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String message = service.deleteMember(id);
            JOptionPane.showMessageDialog(this, message);
            refreshTable();
        }
    }

    private JPanel buildForm(String[] labels, JComponent[] fields) {
        JPanel form = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        return form;
    }
}
