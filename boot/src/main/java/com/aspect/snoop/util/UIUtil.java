package com.aspect.snoop.util;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class UIUtil {

    public static void waitForInput(JDialog view) {
        while (view.isShowing() ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
        }
    }

    public static void showErrorMessage(JDialog dialog, String msg) {
        JOptionPane.showMessageDialog(dialog, msg, "JavaSnoop", JOptionPane.ERROR_MESSAGE);
    }

    public static void showErrorMessage(JFrame frame, String msg) {
        JOptionPane.showMessageDialog(frame, msg, "JavaSnoop", JOptionPane.ERROR_MESSAGE);
    }

    public static void pause(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException ex) { }
    }

    public static File getFileSelection(Dialog parent, boolean onlyDirectories, String startingDir) {

        JFileChooser fc = new JFileChooser(new File(startingDir));
        fc.setDialogTitle("Select directory to dump source into");
        if ( onlyDirectories )
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rc = fc.showOpenDialog(parent);
        if (rc == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        
        return null;
    }

    public static File getFileSelection(JFrame parent, boolean onlyDirectories, String startingDir) {

        if (System.getProperty("os.name").toLowerCase().indexOf("mac") != -1) {
            FileDialog fileDialog = new FileDialog(parent, "Select directory to dump source into", FileDialog.LOAD);
            fileDialog.setDirectory(startingDir);
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null) {
                return new File(fileDialog.getFile());
            }
        } else {
            JFileChooser fc = new JFileChooser(new File(startingDir));
            fc.setDialogTitle("Select directory to dump source into");
            if (onlyDirectories)
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int rc = fc.showOpenDialog(parent);
            if (rc == JFileChooser.APPROVE_OPTION) {
                return fc.getSelectedFile();
            }
        }
        return null;
    }
}
