package common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

public class SVNOperations {

    private static boolean authenticated = false;
    private static String repoLink = null;
    private static SVNRepository repository = null;
    private static ISVNAuthenticationManager authManager = null;
    private static long latestRevision = Long.MIN_VALUE;
    private static HashMap<String, HashMap<String, ArrayList<String>>> files = null;

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static String getRepositoryPath() {
        return repoLink;
    }

    public static long getHeadRevision() {
        return latestRevision;
    }

    public static void listFiles(String path) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        Collection entries = repository.getDir(path, SVNRevision.HEAD.getNumber(), null, (Collection) null);
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            String fullName = entry.getName().toLowerCase();
            String baseName = FilenameUtils.getBaseName(entry.getName().toLowerCase());
            if (entry.getKind() == SVNNodeKind.FILE) {
                String ext = FileOperations.getExtension(entry.getName());
                if (!files.containsKey(ext)) {
                    files.put(ext, new HashMap());
                }
                if (!files.get(ext).containsKey(baseName)) {
                    files.get(ext).put(baseName, new ArrayList());
                }
                files.get(ext).get(baseName).add("/" + (path.equals("") ? "" : path + "/") + entry.getName());
            } else if (entry.getKind() == SVNNodeKind.DIR) {
                if (!files.containsKey("FOLDER")) {
                    files.put("FOLDER", new HashMap());
                }
                if (!files.get("FOLDER").containsKey(fullName)) {
                    files.get("FOLDER").put(fullName, new ArrayList());
                }
                files.get("FOLDER").get(fullName).add("/" + (path.equals("") ? "" : path + "/") + entry.getName());
                listFiles((path.equals("")) ? entry.getName() : path + "/" + entry.getName());
            }
        }
    }

    public static HashMap<String, ArrayList<String>> searchFiles(ArrayList<String> filenames) throws SVNException, Exception {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        if (files == null) {
            throw new Exception("File Index Not Built!");
        }
        HashMap<String, ArrayList<String>> results = new HashMap();

        // Searching filenames containing "input filenames", using painfully expensive O(input files*extensions*files of each extension) complexity
        filenames.forEach((file) -> {
            // Iterating through each file
            String fullname = file.toLowerCase();
            String baseName = FilenameUtils.getBaseName(fullname);
            for (Entry<String, HashMap<String, ArrayList<String>>> entry : files.entrySet()) {
                // Iterating through each extension type
                String extension = entry.getKey();
                HashMap<String, ArrayList<String>> fileBucket = entry.getValue();
                for (Entry<String, ArrayList<String>> entry2 : fileBucket.entrySet()) {
                    // Iterating through each file in the specified extension's file bucket
                    String serverFilename = entry2.getKey() + extension;
                    ArrayList<String> locations = entry2.getValue();
                    if (serverFilename.contains(fullname)) {
                        if (!results.containsKey(file)) {
                            results.put(file, new ArrayList());
                        }
                        results.get(file).addAll(locations);
                    }
                }
            }
        });

        return results;
    }

    public static HashMap<String, ArrayList<String>> searchFiles(ArrayList<String> filenames, boolean ignoreExtensions) throws SVNException, Exception {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        if (files == null) {
            throw new Exception("File Index Not Built!");
        }
        HashMap<String, ArrayList<String>> results = new HashMap();

        if (ignoreExtensions) {
            // Do a full index search
            filenames.forEach((file) -> {
                String fullname = file.toLowerCase();
                String baseName = FilenameUtils.getBaseName(fullname);
                for (Entry<String, HashMap<String, ArrayList<String>>> entry : files.entrySet()) {
                    HashMap<String, ArrayList<String>> fileBucket = entry.getValue();
                    if (fileBucket.containsKey(baseName)) {
                        if (!results.containsKey(file)) {
                            results.put(file, new ArrayList());
                        }
                        results.get(file).addAll(fileBucket.get(baseName));
                    }
                }
            });
        } else {
            // Consider only the files having the same extensions as the parent file
            filenames.forEach((file) -> {
                String fullName = file.toLowerCase();
                String extension = FileOperations.getExtension(fullName);
                String baseName = FilenameUtils.getBaseName(fullName);
                if (files.containsKey(extension)) {
                    HashMap<String, ArrayList<String>> fileBucket = files.get(extension);
                    if (fileBucket.containsKey(baseName)) {
                        if (!results.containsKey(file)) {
                            results.put(file, new ArrayList());
                        }
                        results.get(file).addAll(files.get(extension).get(baseName));
                    }
                }
            });
        }
        return results;
    }

    public static String getSVNErrorMessage(SVNException e) {
        String result = "";
        if (e == null) {
            return result;
        }
        result = e.getErrorMessage().getFullMessage();
        int index = result.indexOf(":", result.indexOf((":")) + 1);
        if (index != -1) {
            result = result.substring(index + 1);
        }
        return result.trim().split("\\R+")[0];
    }

    public static boolean session(String url, String username, String password) throws MalformedURLException, SVNException {
        try {
            // Connecting the the SVN Repo Using Specified Credentials
            repoLink = new URL(url).toString();
            while (repoLink.charAt(repoLink.length() - 1) == '/') {
                repoLink = repoLink.substring(0, repoLink.length() - 1);
            }
            DAVRepositoryFactory.setup();
            SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(repoLink));
            authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(authManager);
            latestRevision = repository.getLatestRevision();
            files = new HashMap();
            Collection entries = repository.getDir("", SVNRevision.HEAD.getNumber(), null, (Collection) null);
            authenticated = true;
        } catch (SVNAuthenticationException se) {
            throw se;
        } catch (Exception e) {
            throw e;
        }
        return authenticated;
    }

    public static void destroySession() {

        // Connecting the the SVN Repo Using Specified Credentials
        repoLink = null;
        repository.closeSession();
        repository = null;
        authManager = null;
        latestRevision = Long.MIN_VALUE;
        authenticated = false;
        files = null;
    }

    public static void update(String sourceDirectory, String destinationDirectory, ArrayList<String> files) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        if (files != null && !files.isEmpty()) {
            File[] f = new File[files.size()];
            for (int a = 0; a < files.size(); a++) {
                f[a] = new File(files.get(a).replace(sourceDirectory, destinationDirectory));
            }
            SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            try {
                final SvnUpdate update = svnOperationFactory.createUpdate();
                update.setRevision(SVNRevision.HEAD);
                update.setAllowUnversionedObstructions(false);
                update.setDepth(SVNDepth.EMPTY);
                update.setMakeParents(true);

                for (File file : f) {
                    try {
                        update.addTarget(SvnTarget.fromFile(file));
                    } catch (Exception e) {
                        System.out.println("Failed for " + file.getAbsolutePath());
                    }
                }
                update.run();
            } finally {
                svnOperationFactory.dispose();
            }
        }
    }

    public static void checkout(String destinationPath, SVNDepth depth) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(repoLink)));
            checkout.setSingleTarget(SvnTarget.fromFile(new File(destinationPath)));
            checkout.setDepth(depth);
            checkout.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }

    public static void checkout(String repoSubpath, String destinationPath, SVNDepth depth) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(repoLink + repoSubpath)));
            checkout.setSingleTarget(SvnTarget.fromFile(new File(destinationPath)));
            checkout.setDepth(depth);
            checkout.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }

    public static void export(String destinationPath, SVNDepth depth) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnExport export = svnOperationFactory.createExport();
            export.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(repoLink)));
            export.setSingleTarget(SvnTarget.fromFile(new File(destinationPath)));
            export.setDepth(depth);
            export.setForce(true);
            export.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }

    public static void export(String repoSubpath, String destinationPath, SVNDepth depth) throws SVNException {
        if (!isAuthenticated()) {
            throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "SVN Authentication Failure!"));
        }
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnExport export = svnOperationFactory.createExport();
            export.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(repoLink + repoSubpath)));
            export.setSingleTarget(SvnTarget.fromFile(new File(destinationPath)));
            export.setDepth(depth);
            export.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }
}
