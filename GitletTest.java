import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 * @author William Zhuang
 *         Code adapted from GitletPublicTest by Joseph Moghadam
 * 
 *         Some code adapted from StackOverflow:
 * 
 *         http://stackoverflow.com/questions
 *         /779519/delete-files-recursively-in-java
 * 
 *         http://stackoverflow.com/questions/326390/how-to-create-a-java-string
 *         -from-the-contents-of-a-file
 * 
 *         http://stackoverflow.com/questions/1119385/junit-test-for-system-out-
 *         println
 * 
 */
public class GitletTest {
    private static final String GITLET_DIR = ".gitlet/";
    private static final String TESTING_DIR = "test_files/";

    /* matches either unix/mac or windows line separators */
    private static final String LINE_SEPARATOR = "\r\n|[\r\n]";

    /**
     * Deletes existing gitlet system, resets the folder that stores files used
     * in testing.
     * 
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(GITLET_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f = new File(TESTING_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f.mkdirs();
    }

    /**
     * Tests that init creates a .gitlet directory. Does NOT test that init
     * creates an initial commit, which is the other functionality of init.
     */
    @Test
    public void testBasicInitialize() {
        gitlet("init");
        File f = new File(GITLET_DIR);
        assertTrue(f.exists());
    }

    /**
     * Tests that checking out a file name will restore the version of the file
     * from the previous commit. Involves init, add, commit, and checkout.
     */
    @Test
    public void testBasicCheckout() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String dogFileName = TESTING_DIR + "dog.txt";

        String wugText = "This is a wug.";
        String dogText = "This is a dog.";

        createFile(wugFileName, wugText);
        createFile(dogFileName, dogText);

        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("add", dogFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        writeFile(dogFileName, "This is not a dog.");

        gitlet("checkout", wugFileName);
        gitlet("checkout", dogFileName);

        assertEquals(wugText, getText(wugFileName));
        assertEquals(dogText, getText(dogFileName));
    }

    /**
     * Tests that log prints out commit messages in the right order. Involves
     * init, add, commit, and log.
     */
    @Test
    public void testBasicLog() {
        gitlet("init");
        String commitMessage1 = "initial commit";

        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("add", wugFileName);
        String commitMessage2 = "added wug";
        gitlet("commit", commitMessage2);

        String logContent = gitlet("log");
        assertArrayEquals(new String[] { commitMessage2, commitMessage1 },
                extractCommitMessages(logContent));
    }

    @Test
    public void testBranches() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");

        gitlet("branch", "red");
        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");

        assertEquals("This is a red wug.", getText(wugFileName));
        gitlet("checkout", "blue");
        assertEquals("This is a blue wug.", getText(wugFileName));
        gitlet("checkout", "master");
        assertEquals("This is a wug.", getText(wugFileName));
    }

    @Test
    public void testGlobalLog() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");

        gitlet("branch", "red");
        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");

        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added red wug", "added blue wug", "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    @Test
    public void testRemove() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String dogFileName = TESTING_DIR + "dog.txt";
        
        String wugText = "This is a wug.";
        String dogText = "This is a dog.";

        createFile(wugFileName, wugText);
        createFile(dogFileName, dogText);

        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("add", dogFileName);
        gitlet("commit", "added wug and dog");

        // Remove file, which will unstage.
        gitlet("remove", wugFileName);
        writeFile(wugFileName, "This is a red wug.");
        gitlet("commit", "did not add red wug");
        gitlet("checkout", wugFileName);
        assertEquals("This is a wug.", getText(wugFileName));
        // Remove file again, which prevents inheritance.
    }

    @Test
    public void testFind() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");

        gitlet("branch", "red");
        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");

        String redContent = gitlet("find", "added red wug");
        String blueContent = gitlet("find", "added blue wug");
        String regContent = gitlet("find", "added wug");

        assertEquals("1\n", regContent);
        assertEquals("2\n", blueContent);
        assertEquals("3\n", redContent);
    }

    @Test
    public void testRemoveBranch() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");

        gitlet("branch", "red");
        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");

        gitlet("rm-branch", "blue");
        String output = gitlet("checkout", "blue");
        String[] split = output.split("\n");
        output = split[1];
        assertEquals("File does not exist in the most recent commit, or no such branch exists.", output);

    }

    @Test
    public void testReset() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is a super wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "upgraded to super wug.");
        writeFile(wugFileName, "This is an under wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "downgraded to under wug.");

        assertEquals("This is an under wug.", getText(wugFileName));
        gitlet("reset", "2");
        assertEquals("This is a super wug.", getText(wugFileName));
    }

    @Test
    public void testMerge() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is a cool wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added cool wug");
        
        gitlet("branch", "red");
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");
        writeFile(wugFileName, "This is a bluer wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added bluer wug");

        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");
        writeFile(wugFileName, "This is a redder wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added redder wug");

        assertEquals("This is a redder wug.", getText(wugFileName));
        String output = gitlet("merge", "blue");
        assertEquals("This is a bluer wug.", getText(wugFileName));
    }

    @Test
    public void testRebase() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");                      // Commit 1
        writeFile(wugFileName, "This is a cool wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added cool wug");                 // Commit 2
        
        gitlet("branch", "red");
        gitlet("branch", "blue");
        gitlet("checkout", "blue");
        writeFile(wugFileName, "This is a blue wug.");      
        gitlet("add", wugFileName);
        gitlet("commit", "added blue wug");                 // Commit 3
        writeFile(wugFileName, "This is a bluer wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added bluer wug");                // Commit 4

        gitlet("checkout", "red");
        writeFile(wugFileName, "This is a red wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added red wug");                  // Commit 5
        writeFile(wugFileName, "This is a redder wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added redder wug");               // Commit 6

        gitlet("rebase", "blue");
        File f = new File(GITLET_DIR +"commit7"); 
        assertTrue(f.exists());
        assertEquals("This is a redder wug.", getText(GITLET_DIR + "commit8/test_files/wug.txt"));
        assertEquals("This is a red wug.", getText(GITLET_DIR + "commit7/test_files/wug.txt"));
    }

    /**
     * Convenience method for calling Gitlet's main. Anything that is printed
     * out during this call to main will NOT actually be printed out, but will
     * instead be returned as a string from this method.
     * 
     * Prepares a 'yes' answer on System.in so as to automatically pass through
     * dangerous commands.
     * 
     * The '...' syntax allows you to pass in an arbitrary number of String
     * arguments, which are packaged into a String[].
     */
    private static String gitlet(String... args) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
        try {
            /*
             * Below we change System.out, so that when you call
             * System.out.println(), it won't print to the screen, but will
             * instead be added to the printingResults object.
             */
            System.setOut(new PrintStream(printingResults));

            /*
             * Prepares the answer "yes" on System.In, to pretend as if a user
             * will type "yes". You won't be able to take user input during this
             * time.
             */
            String answer = "yes";
            InputStream is = new ByteArrayInputStream(answer.getBytes());
            System.setIn(is);

            /* Calls the main method using the input arguments. */
            Gitlet.main(args);

        } finally {
            /*
             * Restores System.out and System.in (So you can print normally and
             * take user input normally again).
             */
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return printingResults.toString();
    }

    /**
     * Returns the text from a standard text file (won't work with special
     * characters).
     */
    private static String getText(String fileName) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Creates a new file with the given fileName and gives it the text
     * fileText.
     */
    private static void createFile(String fileName, String fileText) {
        File f = new File(fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(fileName, fileText);
    }

    /**
     * Replaces all text in the existing file with the given text.
     */
    private static void writeFile(String fileName, String fileText) {
        FileWriter fw = null;
        try {
            File f = new File(fileName);
            fw = new FileWriter(f, false);
            fw.write(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file and all files inside it, if it is a directory.
     */
    private static void recursiveDelete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                recursiveDelete(f);
            }
        }
        d.delete();
    }

    /**
     * Returns an array of commit messages associated with what log has printed
     * out.
     */
    private static String[] extractCommitMessages(String logOutput) {
        String[] logChunks = logOutput.split("====");
        int numMessages = logChunks.length - 1;
        String[] messages = new String[numMessages];
        for (int i = 0; i < numMessages; i++) {
            System.out.println(logChunks[i + 1]);
            String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
            messages[i] = logLines[3];
        }
        return messages;
    }

    public static void main(String[] args) {
        jh61b.junit.textui.runClasses(GitletTest.class);
    }
}
