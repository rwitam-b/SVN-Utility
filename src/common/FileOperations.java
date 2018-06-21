package common;

import static common.FileOperations.sanitizeFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class FileOperations {

    public static final String ABSOLUTE_PATH = "A";
    public static final String FILE_NAME = "F";
    public static final String NEWLINE = System.lineSeparator();
    public static final String SORT_ASC = "A";
    public static final String SORT_DESC = "D";
    public static final String RENAME_UP = "U";
    public static final String RENAME_LOW = "L";
    public static final String RENAME_EXT = "E";

    public static void checkDiskWriteAccess(String path) throws Exception {
        File f = new File(path, String.valueOf(System.currentTimeMillis()));
        boolean created = false;
        try {
            if (!f.exists()) {
                f.createNewFile();
                created = true;
            }
            if (!f.setWritable(true)) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception("Disk Permissions Not Available!" + NEWLINE + "Please try running the application in Administrator Mode!");
        } finally {
            if (created) {
                try {
                    f.delete();
                } catch (Exception e) {
                }
            }
        }
    }

    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename).toLowerCase();
    }

    public static String sanitizePath(String path) {
        String output = "";
        if (path == null || path.isEmpty()) {
            return output;
        }
        File f = new File(path);
        try {
            output = f.getCanonicalPath();
        } catch (Exception e) {
            output = f.getAbsolutePath();
        }
        return output;
    }

    public static HashSet<String> sanitizeFilter(HashSet<String> formats) {
        HashSet<String> out = new HashSet();
        if (formats == null || formats.isEmpty()) {
            return out;
        }
        formats.forEach(t -> {
            if (!t.trim().isEmpty()) {
                out.add(t.trim().toLowerCase());
            }
        });
        return out;
    }

    public static boolean isAcceptableFormat(HashSet<String> formats, String filename, boolean include) {
        return formats == null || formats.isEmpty() || (include && formats.contains(getExtension(filename))) || (!include && !formats.contains(getExtension(filename)));
    }

    public static HashMap<String, String> listFilesToDisk(File listingDirectory, String listingType, HashSet<String> formats, boolean include) throws Exception {
        /*
        *    Returning HashMap will have 2 keys
        *    FILE - (String) - The path to the output file where the file list is stored
        *    FOUND - (String) - "TRUE" or "FALSE" Whether any files have been found or not*/
        HashMap<String, String> result = new HashMap();
        SimpleBooleanProperty found = new SimpleBooleanProperty(false);

        // Input Directory Null Check
        if (listingDirectory == null) {
            throw new Exception("Listing Directory Not Specified!");
        }

        // Listing Type Null Check
        if (listingType == null) {
            throw new Exception("Listing Type Not Specified!");
        }

        // Input Directory Validation
        if (!listingDirectory.exists()) {
            throw new Exception("Listing Directory Does Not Exist!");
        }

        // Input Listing Type Validation
        if (!(listingType.equals(FILE_NAME) || listingType.equals(ABSOLUTE_PATH))) {
            throw new Exception("Listing Type Invalid!");
        }

        // Input Extension Filter Sanitization
        HashSet<String> sanitizedFormats = sanitizeFilter(formats);

        // Creating temporary file FL_<CurrentTimeStamp> under user temp directory
        File out = new File(FileUtils.getTempDirectoryPath(), "FL_" + new Date().getTime() + ".txt");
        result.put("FILE", out.getCanonicalPath());

        // Disk Access Check
        try {
            out.createNewFile();
            if (!out.setWritable(true)) {
                throw new Exception();
            }
            out.delete();
        } catch (Exception e) {
            throw new Exception("Disk Permissions Not Available!" + NEWLINE + "Please Try Running The Application In Administrative Mode!");
        }

        // Listing Files Into Temporary File "out"
        try {
            Files.walkFileTree(Paths.get(listingDirectory.getCanonicalPath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isAcceptableFormat(sanitizedFormats, file.toFile().getName(), include)) {
                        found.set(true);
                        if (listingType.equals(ABSOLUTE_PATH)) {
                            FileUtils.writeStringToFile(out, file.toFile().getCanonicalPath() + NEWLINE, "UTF-8", true);
                        } else if (listingType.equals(FILE_NAME)) {
                            FileUtils.writeStringToFile(out, file.toFile().getName() + NEWLINE, "UTF-8", true);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
            result.put("FOUND", found.get() ? "TRUE" : "FALSE");
            if (result.get("FOUND").equals("FALSE")) {
                result.put("FILE", "");
                out.delete();
            }
        } catch (Exception e) {
            throw new Exception("Failed During File Listing Process!");
        }
        return result;
    }

    public static ArrayList<String> listFilesToArray(File listingDirectory, String listingType, HashSet<String> formats, boolean include) throws Exception {

        ArrayList<String> result = new ArrayList();

        // Input Directory Null Check
        if (listingDirectory == null) {
            throw new Exception("Listing Directory Not Specified!");
        }

        // Listing Type Null Check
        if (listingType == null) {
            throw new Exception("Listing Type Not Specified!");
        }

        // Input Directory Validation
        if (!listingDirectory.exists()) {
            throw new Exception("Listing Directory Does Not Exist!");
        }

        // Input Listing Type Validation
        if (!(listingType.equals(FILE_NAME) || listingType.equals(ABSOLUTE_PATH))) {
            throw new Exception("Listing Type Invalid!");
        }

        // Input Extension Filter Sanitization
        HashSet<String> sanitizedFormats = sanitizeFilter(formats);

        // Listing Files Into Memory
        try {
            Files.walkFileTree(Paths.get(listingDirectory.getCanonicalPath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isAcceptableFormat(sanitizedFormats, file.toFile().getName(), include)) {
                        if (listingType.equals(ABSOLUTE_PATH)) {
                            result.add(file.toFile().getCanonicalPath());
                        } else if (listingType.equals(FILE_NAME)) {
                            result.add(file.toFile().getName());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (Exception e) {
            throw new Exception("Failed During File Listing Process!");
        }
        return result;
    }

    public static HashMap<String, ArrayList<String>> listFilesByTypes(File listingDirectory, String listingType, HashSet<String> formats, boolean include) throws Exception {

        HashMap<String, ArrayList<String>> result = new HashMap();

        // Input Directory Null Check
        if (listingDirectory == null) {
            throw new Exception("Listing Directory Not Specified!");
        }

        // Listing Type Null Check
        if (listingType == null) {
            throw new Exception("Listing Type Not Specified!");
        }

        // Input Directory Validation
        if (!listingDirectory.exists()) {
            throw new Exception("Listing Directory Does Not Exist!");
        }

        // Input Listing Type Validation
        if (!(listingType.equals(FILE_NAME) || listingType.equals(ABSOLUTE_PATH))) {
            throw new Exception("Listing Type Invalid!");
        }

        // Input Extension Filter Sanitization
        HashSet<String> sanitizedFormats = sanitizeFilter(formats);

        // Listing Files Into Memory
        try {
            Files.walkFileTree(Paths.get(listingDirectory.getCanonicalPath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    File temp = file.toFile();
                    String ext = getExtension(temp.getName());
                    if (isAcceptableFormat(sanitizedFormats, temp.getName(), include)) {
                        if (!result.containsKey(ext)) {
                            result.put(ext, new ArrayList());
                        }
                        if (listingType.equals(ABSOLUTE_PATH)) {
                            result.get(ext).add(temp.getCanonicalPath());
                        } else if (listingType.equals(FILE_NAME)) {
                            result.get(ext).add(temp.getName());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (Exception e) {
            throw new Exception("Failed During File Listing Process!");
        }
        return result;
    }

    public static ArrayList<String> getDirectoryList(File source, ArrayList<String> fileList) throws Exception {

        // HashSet to get unique directory paths
        HashSet<String> uniqueDirectories = new HashSet();

        // Getting parent directories from the filenames
        for (int a = 0; a < fileList.size(); a++) {
            uniqueDirectories.add(new File(fileList.get(a)).getParent());
        }

        // Storing back to ArrayList
        fileList = new ArrayList(uniqueDirectories);

        // Getting intermediate directories between source path and folders containing the files
        for (String str : fileList) {
            File temp = new File(str);
            while (!temp.getParent().equals(source.getCanonicalPath())) {
                uniqueDirectories.add(temp.getParent());
                temp = temp.getParentFile();
            }
        }

        // Storing back to ArrayList
        fileList = new ArrayList(uniqueDirectories);

        // Sorting according to ascending order
        Collections.sort(fileList);

        return fileList;
    }

    public static void sortTextFileLex(String absoluteFilePath, String sortOrder) throws Exception {
        // Input Directory Null Check
        if (absoluteFilePath == null) {
            throw new Exception("File Path Not Specified!");
        }

        // Sort Order Will Default To "Ascending" If null
        if (sortOrder == null) {
            sortOrder = SORT_ASC;
        }

        // Sort Order Validation
        if (!(sortOrder.equals(SORT_ASC) || sortOrder.equals(SORT_DESC))) {
            throw new Exception("Invalid Sort Order!");
        }

        // File Validation
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            throw new Exception("File Does Not Exist!");
        }

        // Disk Access Check
        if (!file.canRead() || !file.canWrite()) {
            throw new Exception("Disk Permissions Not Available!" + NEWLINE + "Please Try Running The Application In Administrative Mode!");
        }

        // Reading file contents into memory
        List<String> lines = FileUtils.readLines(file, "UTF-8");

        // Sorting list contents based on sort type
        if (sortOrder.equals(SORT_ASC)) {
            Collections.sort(lines, (str1, str2) -> {
                return str1.trim().toLowerCase().compareTo(str2.trim().toLowerCase());
            });
        } else if (sortOrder.equals(SORT_DESC)) {
            Collections.sort(lines, (str1, str2) -> {
                return str2.trim().toLowerCase().compareTo(str1.trim().toLowerCase());
            });
        }

        // Writing back contents to file
        FileUtils.writeLines(file, "UTF-8", lines, NEWLINE, false);
    }

    public static String rename(String absoluteFilePath, String renameCase, String renameExtension) throws Exception {

        // Input File Null Check
        if (absoluteFilePath == null) {
            throw new Exception("File Path Not Specified!");
        }

        // Rename Type Null Check
        if (renameCase == null) {
            throw new Exception("Renaming Case Not Specified!");
        }

        // Renaming Case Validation
        if (!(renameCase.equals(RENAME_LOW) || renameCase.equals(RENAME_UP) || renameCase.equals(RENAME_EXT))) {
            throw new Exception("Invalid Renaming Case!");
        }

        // File Validation
        File original = new File(absoluteFilePath);
        if (!original.exists()) {
            throw new Exception("File Does Not exist!");
        }

        // Disk Access Check
        if (!original.canRead() || !original.canWrite()) {
            throw new Exception("Disk Permissions Not Available!" + NEWLINE + "Please Try Running The Application In Administrative Mode!");
        }

        // Getting Renamed
        String newName = "";
        if (renameCase.equals(RENAME_LOW)) {
            newName = original.getName().toLowerCase(Locale.getDefault());
        } else if (renameCase.equals(RENAME_UP)) {
            newName = original.getName().toUpperCase();
        } else if (renameCase.equals(RENAME_EXT)) {
            newName = FilenameUtils.getBaseName(original.getName()) + "." + renameExtension;
        }

        File changed = new File(original.getParent(), newName);

        // Checking if rename required
        if (original.getName().equals(changed.getName())) {
            return original.getName();
        }

        // Executing file rename
        boolean status = original.renameTo(changed);
        return status ? changed.getName() : original.getName();
    }

    public static int trimFileSpaces(String absoluteFilePath, boolean removeAllBlankLines) throws Exception {
        int changes = 0;

        // Input File Null Check
        if (absoluteFilePath == null) {
            throw new Exception("File Path Not Specified!");
        }

        // File Validation
        File original = new File(absoluteFilePath);
        if (!original.exists()) {
            throw new Exception("File Does Not exist!");
        }

        // Disk Access Check
        if (!original.canRead() || !original.canWrite()) {
            throw new Exception("Disk Permissions Not Available!" + NEWLINE + "Please Try Running The Application In Administrative Mode!");
        }

        original.setWritable(true);
        List<String> lines = FileUtils.readLines(original, "UTF-8");

        // Removing preceeding blank lines
        while (lines.size() > 0 && lines.get(0).trim().equals("")) {
            lines.remove(0);
            changes++;
        }

        // Removing trailing blank lines
        while (lines.size() > 0 && lines.get(lines.size() - 1).trim().equals("")) {
            lines.remove(lines.size() - 1);
            changes++;
        }

        if (removeAllBlankLines) {
            // Removing all blank lines
            for (int a = lines.size() - 1; a >= 0; a--) {
                if (lines.get(a).trim().equals("")) {
                    lines.remove(a);
                    changes++;
                }
            }
        }

        // Writing back changes to the file
        FileUtils.writeLines(original, "UTF-8", lines, FileOperations.NEWLINE, false);
        return changes;
    }
}
