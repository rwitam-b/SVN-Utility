package processor;

import common.FileOperations;
import common.SVNOperations;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;

public class SVNCheckout {

    private TextArea outputLogs;

    public SVNCheckout(TextArea outputLogs) throws Exception {
        if (!SVNOperations.isAuthenticated()) {
            throw new Exception("SVN Authentication Failure!");
        }
        this.outputLogs = outputLogs;
    }

    public void doCheckout(String type, String destinationPath, SVNDepth depth) throws Exception {
        try {
            // Checkout/Export All Operation
            String path = FileOperations.sanitizePath(destinationPath);
            Platform.runLater(() -> {
                outputLogs.appendText("Trying to " + type + " items from " + SVNOperations.getRepositoryPath() + FileOperations.NEWLINE);
            });
            if (type.toLowerCase().equals("checkout")) {
                SVNOperations.checkout(path, depth);
                Platform.runLater(() -> {
                    outputLogs.appendText("Files successfully checked out to " + path + FileOperations.NEWLINE);
                });
            } else if (type.toLowerCase().equals("export")) {
                SVNOperations.export(path, depth);
                Platform.runLater(() -> {
                    outputLogs.appendText("Files successfully exported to " + path + FileOperations.NEWLINE);
                });
            }
        } catch (SVNException se) {
            throw new Exception(SVNOperations.getSVNErrorMessage(se));
        } catch (Exception e) {
            throw e;
        }
    }

    public void doCheckout(String type, String workingDirectory, String sourcepath, String[] extensionFilters, boolean include) throws Exception {
        try {
            // Checkout/Export With Reference Operation
            Platform.runLater(() -> {
                outputLogs.appendText("Analyzing " + sourcepath + FileOperations.NEWLINE + FileOperations.NEWLINE);
            });
            File source = new File(sourcepath);
            HashSet sanitizedFormats = FileOperations.sanitizeFilter(new HashSet(Arrays.asList(extensionFilters)));

            ArrayList<String> fileList = new ArrayList();
            ArrayList<String> directories = new ArrayList();

            Files.walkFileTree(Paths.get(source.getCanonicalPath()), new SimpleFileVisitor<Path>() {
                @Override
                // Listing out files required to be operated on
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (FileOperations.isAcceptableFormat(sanitizedFormats, file.toFile().getName(), include)) {
                        fileList.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path t, BasicFileAttributes bfa) throws IOException {
                    directories.add(t.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                // Getting list of directories from the source folder (For Empty Checkout)
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });

            // Mapping Filenames To Their Respective Container Directories
            HashMap<String, ArrayList<String>> fileMap = new HashMap();
            for (String filename : fileList) {
                String directory = new File(filename).getParent();
                if (!fileMap.containsKey(directory)) {
                    fileMap.put(directory, new ArrayList());
                }
                fileMap.get(directory).add(filename);
            }

            File sourceFolder = new File(sourcepath);
            File workingCopy = new File(workingDirectory);

            if (type.toLowerCase().equals("checkout")) {
                // Performing Empty Checkout For Each Folder Retreived From The Source Folder
                for (String folder : directories) {
                    try {
                        // Replacing backslashes in the path to forward slashes for use in URL
                        String repoPath = folder.replace(sourceFolder.getCanonicalPath(), "").replaceAll("\\\\", "/");

                        Platform.runLater(() -> {
                            outputLogs.appendText("Processing " + folder + FileOperations.NEWLINE);
                        });

                        // Performimng Empty Checkout For Each Folder
                        SVNOperations.checkout(repoPath, folder.replace(sourceFolder.getCanonicalPath(), workingCopy.getCanonicalPath()), SVNDepth.EMPTY);

                        // Updating Files If Any Present Under That Folder
                        SVNOperations.update(sourceFolder.getCanonicalPath(), workingCopy.getCanonicalPath(), fileMap.get(folder));
                    } catch (SVNException e) {
                        Platform.runLater(() -> {
                            outputLogs.appendText("Failed for " + folder + FileOperations.NEWLINE);
                        });
                    }
                }
            } else if (type.toLowerCase().equals("export")) {
                // Performing Export For Each File Retreived From The Source Folder
                for (String folder : directories) {
                    if (fileMap.containsKey(folder)) {
                        Platform.runLater(() -> {
                            outputLogs.appendText("Processing " + folder + FileOperations.NEWLINE);
                        });
                        for (String file : fileMap.get(folder)) {
                            try {
                                // Replacing backslashes in the path to forward slashes for use in URL
                                String repoPath = file.replace(sourceFolder.getCanonicalPath(), "").replaceAll("\\\\", "/");

                                SVNOperations.export(repoPath, file.replace(sourceFolder.getCanonicalPath(), workingCopy.getCanonicalPath()), SVNDepth.EMPTY);
                            } catch (SVNException e) {
                                Platform.runLater(() -> {
                                    outputLogs.appendText("Failed for " + file + FileOperations.NEWLINE);
                                });
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw e;
        }
    }

    public void doCheckout(String type, String workingDirectory, String[] sourceFiles) throws Exception {
        try {
            // Checkout/Export With Links Option
            Platform.runLater(() -> {
                outputLogs.appendText("Processing Files" + FileOperations.NEWLINE + FileOperations.NEWLINE);
            });
            if (type.toLowerCase().equals("checkout")) {
                SVNOperations.checkout(workingDirectory, SVNDepth.EMPTY);
                SVNOperations.update(SVNOperations.getRepositoryPath(), workingDirectory, new ArrayList(Arrays.asList(sourceFiles)));
            } else if (type.toLowerCase().equals("export")) {
                for (String file : sourceFiles) {
                    try {
                        SVNOperations.export(file.replace(SVNOperations.getRepositoryPath(), ""), file.replace(SVNOperations.getRepositoryPath(), workingDirectory), SVNDepth.EMPTY);
                    } catch (SVNException e) {
                        Platform.runLater(() -> {
                            outputLogs.appendText(SVNOperations.getSVNErrorMessage(e) + FileOperations.NEWLINE);
                        });
                    }
                }
            }

        } catch (Exception e) {
            throw e;
        }
    }
}
