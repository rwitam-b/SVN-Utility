package processor;

import common.FileOperations;
import common.SVNOperations;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNException;

public class SVNSearch {

    private TextArea outputLogs;

    public SVNSearch(TextArea outputLogs) throws Exception {
        if (!SVNOperations.isAuthenticated()) {
            throw new Exception("SVN Authentication Failure!");
        }
        this.outputLogs = outputLogs;
    }

    public void execute(String copyPath, String[] files, boolean copyFiles) throws Exception {
        try {
            // Constructing "filename" array from input list
            Platform.runLater(() -> {
                outputLogs.appendText("Analyzing File List....." + FileOperations.NEWLINE);
            });
            ArrayList<String> filenames = new ArrayList();
            for (String file : files) {
                try {
                    if (!file.trim().isEmpty()) {
                        File f = new File(file);
                        if (copyFiles && f.exists() && f.isFile()) {
                            filenames.add(f.getName());
                        } else if (!copyFiles) {
                            filenames.add(f.getName());
                        } else {
                            Platform.runLater(() -> {
                                outputLogs.appendText("Skipping " + f.getAbsolutePath() + FileOperations.NEWLINE);
                            });
                        }
                    }
                } catch (Exception e) {
                }
            }

            // Preparing log files
            Date D = new Date();
            String logFile = new SimpleDateFormat("'search_'yyyy_MM_dd_HH_mm'.txt'").format(D);
            String copiedFile = new SimpleDateFormat("'copied_'yyyy_MM_dd_HH_mm'.txt'").format(D);
            String skippedFile = new SimpleDateFormat("'skipped_'yyyy_MM_dd_HH_mm'.txt'").format(D);
            String linksFile = new SimpleDateFormat("'links_'yyyy_MM_dd_HH_mm'.txt'").format(D);
            File log = new File(copyPath, logFile);
            File copy = new File(copyPath, copiedFile);
            File skip = new File(copyPath, skippedFile);
            File link = new File(copyPath, linksFile);
            ArrayList<String> logs, copied, skipped, links;

            Platform.runLater(() -> {
                outputLogs.appendText("Searching For Files....." + FileOperations.NEWLINE);
            });

            HashMap<String, ArrayList<String>> found = SVNOperations.searchFiles(filenames, true);

            Platform.runLater(() -> {
                outputLogs.appendText(FileOperations.NEWLINE + "Processing....." + FileOperations.NEWLINE);
            });
            logs = new ArrayList();
            links = new ArrayList();
            if (copyFiles) {
                copied = new ArrayList();
                skipped = new ArrayList();
                for (String file : files) {
                    File f = new File(file);
                    logs.add(f.getName());
                    if (found.containsKey(f.getName())) {
                        if (found.get(f.getName()).size() == 1) {
                            try {
                                FileUtils.copyFileToDirectory(f, new File(copyPath, found.get(f.getName()).get(0)).getParentFile());
                                copied.add(f.getName());
                                copied.add(SVNOperations.getRepositoryPath() + found.get(f.getName()).get(0));
                                logs.add(SVNOperations.getRepositoryPath() + found.get(f.getName()).get(0));
                                links.add(SVNOperations.getRepositoryPath() + found.get(f.getName()).get(0));
                                copied.add(FileOperations.NEWLINE);
                            } catch (Exception e) {
                                logs.add("Failed To Copy File!");
                            }
                        } else {
                            skipped.add(f.getName());
                            found.get(f.getName()).stream().forEachOrdered((path) -> {
                                logs.add(SVNOperations.getRepositoryPath() + path);
                                skipped.add(SVNOperations.getRepositoryPath() + path);
                            });
                            skipped.add(FileOperations.NEWLINE);
                        }
                        logs.add("Find Count: " + found.get(f.getName()).size());
                    } else {
                        logs.add("Not Found!");
                    }
                    logs.add(FileOperations.NEWLINE);
                }
                if (copied.size() > 0) {
                    Platform.runLater(() -> {
                        outputLogs.appendText("Writing Copy Logs To " + copy.getAbsolutePath() + FileOperations.NEWLINE);
                    });
                    FileUtils.writeLines(copy, "UTF-8", copied, FileOperations.NEWLINE, false);
                }
                if (skipped.size() > 0) {
                    Platform.runLater(() -> {
                        outputLogs.appendText("Writing Skipped Logs To " + skip.getAbsolutePath() + FileOperations.NEWLINE);
                    });
                    FileUtils.writeLines(skip, "UTF-8", skipped, FileOperations.NEWLINE, false);
                }
            } else {
                filenames.stream().map((filename) -> {
                    logs.add(filename);
                    return filename;
                }).map((filename) -> {
                    if (found.containsKey(filename)) {
                        found.get(filename).forEach((path) -> {
                            logs.add(SVNOperations.getRepositoryPath() + path);
                            links.add(SVNOperations.getRepositoryPath() + path);
                        });
                        logs.add("Find Count: " + found.get(filename).size());
                    } else {
                        logs.add("Not Found!");
                    }
                    return filename;
                }).forEachOrdered((_item) -> {
                    logs.add(FileOperations.NEWLINE);
                });
            }

            Platform.runLater(() -> {
                outputLogs.appendText("Writing Search Logs To " + log.getAbsolutePath() + FileOperations.NEWLINE);
            });
            FileUtils.writeLines(log, "UTF-8", logs, FileOperations.NEWLINE, false);
            if (links.size() > 0) {
                Platform.runLater(() -> {
                    outputLogs.appendText("Writing Links To " + skip.getAbsolutePath() + FileOperations.NEWLINE);
                });
                FileUtils.writeLines(link, "UTF-8", links, FileOperations.NEWLINE, false);
            }
        } catch (SVNException se) {
            throw new Exception(SVNOperations.getSVNErrorMessage(se));
        } catch (Exception e) {
            throw e;
        }
    }
}
