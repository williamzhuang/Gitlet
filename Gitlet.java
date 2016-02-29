import java.io.Serializable;
import java.io.File;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.Console;
import java.util.Scanner;
import java.io.DataOutputStream;
import java.io.DataInputStream;

// @Author William Zhuang
// Citations: http://www.mkyong.com/java/how-to-create-directory-in-java/
// http://beginnersbook.com/2013/12/how-to-serialize-hashmap-in-java/
// http://www.mkyong.com/java/how-to-read-file-from-jav/ -bufferedreader-example/
// http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
// http://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
// http://stackoverflow.com/questions/5287538/how-to-get-basic-user-input-for-java
// http://stackoverflow.com/questions/1510520/how-to-compare-the-contents-of-two-text-files-and-return-same-content-or-diff


public class Gitlet implements Serializable {
	
	private static HashMap<Integer, Node> nodeTree;
	private static HashMap<String, Integer> messages;
	private static HashSet<String> added;
	private static HashSet<String> removed;
	private static HashSet<String> unstaged;
	private static HashMap<String, Integer> branches;
	private static HashMap<String, ArrayList<Integer>> branchHistory;
	private static String curBranch;
	private static int head;
	private static int commitNum;
	private static boolean initialized;

	public static void main(String[] args) {

		initializeStructures();
		File initialization = new File(".gitlet/initialized.ser");
		if (initialization.exists()) {
			initialized = deserializeBool(".gitlet/initialized.ser");
			readObjects();
		}

		String command = args[0];
		switch (command) {
			/** Creates a new gitlet version control system in the current directory. 
			  * Generates initial tial commit", null, time));commit. */
			case "init":

				DateFormat dateFormatInit = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Calendar calenit = Calendar.getInstance();
				String timeInit = dateFormatInit.format(calenit.getTime());

				File file = new File(".gitlet/");
				if (!file.exists() && !file.isDirectory()) {
					if (file.mkdir()) {
						File commit0 = new File(".gitlet/commit0");
						commit0.mkdir();
						Node initNode = new Node(-1, "initial commit", new HashSet<String>(), timeInit);
						nodeTree.put(0, initNode);
						messages.put("initial commit", 0);
						head = 0;
						commitNum = 0;
						ArrayList<Integer> firstHist = new ArrayList<Integer>();
						firstHist.add(0);
						branches.put("master", 0);
						branchHistory.put("master", firstHist);

					} else {
						System.out.println("Failed to generate directory.");
					}
				} else {
					System.out.println("A gitlet version control system already exists in the current directory.");
				}

				curBranch = "master";
				initialized = true;
				writeObjects();
				break;
			/** Indicates the file which is to be included in the upcoming commit and
 			  * marks it as staged. */
 			case "add":
 				if (args.length <= 1) {
 					System.out.println("Please provide the file to be added.");
 					break;
 				}

 				File toAdd = new File(args[1]);
 				if (!toAdd.exists()) {
 					System.out.println("File does not exist.");
 				}

 				int lastCommit = nodeTree.get(head).parent;
 				File prevAdd = new File(".gitlet/commit" + Integer.toString(lastCommit) + "/" + args[1]);

 				if (prevAdd.exists() && !prevAdd.isDirectory()) {
 					if (equalFile(args[1], ".gitlet/commit" + Integer.toString(lastCommit) + "/" + args[1])) {
 						System.out.println("File has not been modified since the last commit.");
 						break;
 					}
 				}
 				
 				if (removed.contains(args[1])) {
 					removed.remove(args[1]);
 					writeObjects();
 					break;
 				}

 				added.add(args[1]);
 				writeObjects();
 				break;
 			/** Saves a snapshot of staged files and files from previous commits that
 			  * can be restored at a later time. */
 			case "commit":
 				if (args.length <= 1) {
 					System.out.println("Please provide a commit message.");
 					break;
 				}

 				if (added.isEmpty()) {
 					System.out.println("No changes added to the commit.");
 					break;
 				}
 				String msg = args[1];
 				commitNum += 1;

 				// Create a new folder to store the commit.
 				HashSet<String> prevFiles = new HashSet<String>();
 				prevFiles.addAll(nodeTree.get(head).fileNames);
 				String prevFolder = ".gitlet/commit" + Integer.toString(head) + "/";

 				// Copy previous folder over.
 				File fromCopy = null;
 				File toCopy = null;
 				for (String x : prevFiles) {
 					if ((removed.contains(x)) || (added.contains(x))) {
 						continue;
 					}
 					fromCopy = new File(prevFolder + x);
 					toCopy = new File(".gitlet/commit" + Integer.toString(commitNum) +"/" + x);
 					try {
 						if (toCopy.getParentFile() != null) {
 							toCopy.getParentFile().mkdirs();
 						}
 						
 						Files.copy(fromCopy.toPath(), toCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
 					} catch (IOException e) {

 					}
 				}

 				// Adding files that have been added.
 				File edited = null;
 				File editedTo = null;
 				for (String x : added) {
 					prevFiles.add(x);
 					edited = new File(x);
 					editedTo = new File(".gitlet/commit" + Integer.toString(commitNum) +"/" + x);
 					try {
 						if (editedTo.getParentFile() != null) {
 							editedTo.getParentFile().mkdirs();
 						}
 						Files.copy(edited.toPath(), editedTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
 					} catch (IOException e) {

 					}
 				}

 				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
 				Calendar calen = Calendar.getInstance();
				String time = dateFormat.format(calen.getTime());

 				nodeTree.put(commitNum, new Node(head, msg, prevFiles, time));
 				messages.put(msg, commitNum);

 				head = commitNum;
 				branches.put(curBranch, head);

 				// Updates history of the branch.
 				ArrayList<Integer> tempHistory = new ArrayList<Integer>();
 				tempHistory.addAll(branchHistory.get(curBranch));
 				tempHistory.add(head);
 				branchHistory.put(curBranch, tempHistory);

 				added = new HashSet<String>();
 				removed = new HashSet<String>();
 				unstaged = new HashSet<String>();
 				writeObjects();

 				break;
 			/** Marks the file for removal, preventing it from being inherited. Also
 			  * unstages the file if it was staged. */
 			case "remove":
 				if (args.length <= 1) {
 					System.out.println("Please provide a file name to be removed.");
 				}

 				String filename = args[1];
 				// Check if file was included in previous commit.
 				HashSet<String> prevCommit = nodeTree.get(head).fileNames;

 				if (added.contains(filename)) {
 					added.remove(filename);
 					writeObjects();
 					break;
 				}

 				if (prevCommit.contains(filename)) {
 					removed.add(filename);
 					writeObjects();
 					break;
 				}

 				System.out.println("No reason to remove the file.");
 				writeObjects();
 				break;
 			/** Displays information about each commit backwards along the commit tree
 			  * until the initial commit. */	
 			case "log":
 				int pointer = head;
 				Node temp = null;
 				while (pointer != -1) {
 					temp = nodeTree.get(pointer);
 					System.out.println("====");
 					System.out.println("Commit " + pointer + ".");
 					System.out.println(temp.time);
 					System.out.println(temp.commitMessage + "\n");
 					pointer = temp.parent;
 				}
 				writeObjects();
 				break;
 			/** displays information about all commits ever made in no particular order. */
 			case "global-log":
 				for (Integer x : nodeTree.keySet()) {
 					System.out.println("====");
 					System.out.println("Commit " + x + ".");
 					System.out.println(nodeTree.get(x).time);
 					System.out.println(nodeTree.get(x).commitMessage + "\n");

 				}
 				writeObjects();
 				break;
 			/** Prints out the ID of the commit with the given commit message */
 			case "find":
 				if (args.length <= 1) {
 					System.out.println("Please provide a commit message.");
 				}

 				String comMsg = args[1];
 				int id = messages.get(comMsg);
 				System.out.println(id);
 				writeObjects();
 				break;
 			/** Displays what branches currently exists and marks the current branch with a *. 
 			  * Also displays which files have been staged or marked for removal. */
 			case "status":
 				// Displays branches.
 				System.out.println("=== Branches ===");
 				for (String x : branches.keySet()) {
 					if (x.equals(curBranch)) {
 						System.out.println(x + "*");
 					} else {
 						System.out.println(x);
 					}
 					
 				}

 				// Displays staged files.
 				System.out.println("\n=== Staged Files ===");
 				for (String x : added) {
 					System.out.println(x);
 				}

 				// Displays files marked for removal.
 				System.out.println("\n=== Files Marked for Removal ===");
 				for (String x : removed) {
 					System.out.println(x);
 				}
 				writeObjects();
 				break;
 			/** Restores files to previous states. */
 			case "checkout":
 				if (args.length <= 1) {
 					System.out.println("Please provide a valid argument.");
 					break;
 				}

 				if (!checkDangerous()) {
 					break;
 				}

 				String checkoutArg = args[1];
 				// Restores file in this commit.
 				if (args.length == 3) {
 					int commitID = Integer.parseInt(args[1]);
 					checkoutArg = args[2];
 					File destCheck = new File(checkoutArg);
 					File source = new File(".gitlet/commit" + Integer.toString(commitID) + "/" + checkoutArg);
 					if (destCheck.exists() && source.exists()) {
 						try {
 							if (destCheck.getParentFile() != null) {
 								destCheck.getParentFile().mkdirs();
 							}
 							
 							Files.copy(source.toPath(), destCheck.toPath(), StandardCopyOption.REPLACE_EXISTING);
 						} catch (IOException e) {

 						}
 					} else if (nodeTree.get(commitID) == null) {
 						System.out.println("No commit with that id exists.");
 						break;
 					} else {
 						System.out.println("File does not exist in that commit.");
 						break;
 					}
 					writeObjects();
 					break;
 				}
 				// Check if it's a branch first.
 				if (checkoutArg.equals(curBranch)) {
 					System.out.println("No need to checkout the current branch.");
 					break;
 				}

 				if (branches.get(checkoutArg) != null) {
 					int headPointer = branches.get(checkoutArg);
 					HashSet<String> fileNamesCpy = new HashSet<String>();
 					fileNamesCpy.addAll(nodeTree.get(headPointer).fileNames);
					
					File destBranch = null;
					File toCopyBranch = null;
 					for (String x : fileNamesCpy) {
 						toCopyBranch = new File(".gitlet/commit" + Integer.toString(headPointer) + "/" + x);
 						destBranch = new File(x);
 						try {
 							if (destBranch.getParentFile() != null) {
 								destBranch.getParentFile().mkdirs();
 							}	
 							Files.copy(toCopyBranch.toPath(), destBranch.toPath(), StandardCopyOption.REPLACE_EXISTING);
 						} catch (IOException e) {
 							
 						}
 					}

 					curBranch = checkoutArg;
 					head = branches.get(curBranch);
 					writeObjects();
 					break;
 				}
 				// Check if it's a file.
 				File checkFile = new File(checkoutArg);
 				File sourceFile = new File(".gitlet/commit" + Integer.toString(commitNum) + "/" + checkoutArg);
 				if (sourceFile.exists()) {
 					try {
 						Files.delete(checkFile.toPath());
 						if (checkFile.getParentFile() != null) {
 							checkFile.getParentFile().mkdirs();
 						}
 						
 						Files.copy(sourceFile.toPath(), checkFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
 					} catch (IOException e) {
 						
 					}
 				} else {
 					System.out.println("File does not exist in the most recent commit, or no such branch exists.");
 				}
 				writeObjects();
 				break;
 			/** Creates a new branch with the given name. */
 			case "branch":
 				if (args.length <= 1) {
 					System.out.println("Please provide a branch name.");
 					break;
 				}

 				if (branches.containsKey(args[1])) {
 					System.out.println("A branch with that name already exists.");
 					break;
 				}

 				branches.put(args[1], head);
 				branchHistory.put(args[1], branchHistory.get(curBranch));
 				writeObjects();

 				break;
 			/** Deletes the branch with the given name. */
 			case "rm-branch":
 				if (args.length <= 1) {
 					System.out.println("Please provide a branch name.");
 					break;
 				}

 				if (args[1].equals(curBranch)) {
 					System.out.println("Cannot remove the current branch.");
 				}

 				if (branches.get(args[1]) != null) {
 					branches.remove(args[1]);
 				} else {
 					System.out.println("A branch with that name does not exist.");
 				}
 				writeObjects();
 				break;
 			/** Restores all files to their versions in the version with the given commit ID. */
 			case "reset":
 				if (args.length <= 1) {
 					System.out.println("Please provide a commit ID.");
 					break;
 				}

 				if (!checkDangerous()) {
 					break;
 				}

 				HashSet<String> replacers = nodeTree.get(Integer.parseInt(args[1])).fileNames;
 				File replacer = null;
 				File replaced = null;
 				for (String x : replacers) {
 					replacer = new File(".gitlet/commit" + args[1] + "/" + x);
 					replaced = new File(x);
 					try {
 						if (replaced.getParentFile() != null) {
 							replaced.getParentFile().mkdirs();
 						}
 						
 						Files.copy(replacer.toPath(), replaced.toPath(), StandardCopyOption.REPLACE_EXISTING);
 					} catch (IOException e) {

 					}
 				}
				writeObjects();
 				break;
 			/** Merges files from the head of the given branch into the head of the current branch. */
 			case "merge":
 				if (args.length <= 1) {
 					System.out.println("Please provide a branch to merge from.");
 					break;
 				}

 				if (!branches.containsKey(args[1])) {
 					System.out.println("A branch with that name does not exist.");
 					break;
 				}

 				if (args[1].equals(curBranch)) {
 					System.out.println("Cannot merge a branch with itself");
 					break;
 				}

 				if (!checkDangerous()) {
 					break;
 				}

 				int splitPoint = findSplit(args[1], curBranch);

 				// Check for unmodified files in current branch.
 				HashSet<String> curFiles = nodeTree.get(head).fileNames;
 				HashSet<String> copyThese = new HashSet<String>();

 				for (String x : curFiles) {
 					if (!equalFile(x, ".gitlet/commit" + Integer.toString(splitPoint) + "/" + x)) {
 						copyThese.add(x);
 					}
 				}

 				// Move those same files from the other branch to this branch.
 				File mergeSource = null;
 				File mergeDest = null;
 				for (String x : copyThese) {
 					mergeDest = new File(x);
 					mergeSource = new File(".gitlet/commit" + Integer.toString(branches.get(args[1])) + "/" + x);
 					try {
 						if (mergeDest.getParentFile() != null) {
 							mergeDest.getParentFile().mkdirs();
 						}
 						
 						Files.copy(mergeSource.toPath(), mergeDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
 					} catch (IOException e) {

 					}
 				}
 				writeObjects();
 				break;
 			/** Finds the split point of the current branch and reattaches all files after that point to 
 			  * the end of the given branch. */
 			case "rebase":
 				if (args.length <= 1) {
 					System.out.println("Please provide a branch to rebase.");
 					break;
 				}

 				if (!branches.containsKey(args[1])) {
 					System.out.println("A branch with that name does not exist.");
 					break;
 				}

 				if (args[1].equals(curBranch)) {
 					System.out.println("Cannot rebase a branch with itself");
 					break;
 				}

 				if (!checkDangerous()) {
 					break;
 				}

 				int splitStart = findSplit(args[1], curBranch);
 				if (splitStart == -1) {
 					head = branches.get(args[1]);
 				} else {
 					
 					combineHistories(args[1], curBranch); 
 				}
 				writeObjects();
 				break;
 			/** Does what rebase does but allows user to change the commit message or skip replaying the commit. */
 			case "i-rebase":
 				if (args.length <= 1) {
 					System.out.println("Please provide a branch to rebase.");
 					break;
 				}

 				if (!branches.containsKey(args[1])) {
 					System.out.println("A branch with that name does not exist.");
 					break;
 				}

 				if (args[1].equals(curBranch)) {
 					System.out.println("Cannot rebase a branch with itself");
 					break;
 				}

 				if (!checkDangerous()) {
 					break;
 				}

 				combineHistoriesI(args[1], curBranch);

 				writeObjects();
 				break;
 		}
 	}

 	private static void copyAll(Node oldNode, int sourceCommit, int destCommit) {
 		HashSet<String> files = oldNode.fileNames;
 		File from = null;
 		File to = null;
 		for (String x : files) {
 			from = new File(".gitlet/commit" + Integer.toString(sourceCommit) + "/" + x);
 			to = new File(".gitlet/commit" + Integer.toString(destCommit) + "/" + x);
 			try {
 				if (to.getParentFile() != null) {
 					to.getParentFile().mkdirs();
 				}
 				
 				Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
 			} catch (IOException e) {

 			}
 		}
 	}

 	private static boolean checkDangerous() {
 		Scanner scanner = new Scanner(System.in);
 		System.out.println("Warning: The command you entered may alter the files in your working directory. " + 
 						   "Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
 		String yesno = scanner.next();
 		if (yesno.equals("yes")) {
 			return true;
 		} else {
 			return false;
 		}
 	}

 	private static void combineHistoriesI(String fromBranch, String toBranch) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cale = Calendar.getInstance();
		String time = dateFormat.format(cale.getTime());

 		ArrayList<Integer> fromHist = branchHistory.get(fromBranch);
 		ArrayList<Integer> toHist = branchHistory.get(toBranch);
 		ArrayList<Integer> result = new ArrayList<Integer>();
 		result.addAll(fromHist);

 		Scanner scanner = new Scanner(System.in);
 		String newMsg = null;

 		Node oldNode = null;
 		Node newNode = null;
 		int oldParent = branches.get(fromBranch);

 		for (int x : toHist) {
 			if (!result.contains(x)) {
 				result.add(x);
 				System.out.println("Currently replaying:");
 				oldNode = nodeTree.get(x);
 				System.out.println("====");
 				System.out.println("Commit " + x + ".");
 				System.out.println(oldNode.time);
 				System.out.println(oldNode.commitMessage + "\n");
 				String response = "killswitch";
 				while ((!response.equals("c") && !response.equals("s") && !response.equals("m")) || (response.equals("s") &&
 					    (toHist.indexOf(x) == 0) && (toHist.indexOf(x) == toHist.size() - 1))) {
 					System.out.println("Would you like to (c)ontinue, (s)kip this commit, or change this commit's (m)essage?");
 					response = scanner.next();
 				}
 	
 				if (response.equals("c")) {
 					newNode = new Node(oldNode, oldParent, time);
 					commitNum += 1;
 					nodeTree.put(commitNum, newNode);
 					copyAll(oldNode, x, commitNum);
 					oldParent = commitNum;
 					head = commitNum;
 					branches.put(curBranch, head);
 					continue;
 				} else if (response.equals("s")) {
 					result.remove(x);
 				} else {
 					System.out.println("Please enter a new message for this commit.");
 					newMsg = scanner.next();

 					newNode = new Node(oldNode, oldParent, newMsg, time);
 					commitNum += 1;
 					nodeTree.put(commitNum, newNode);
 					copyAll(oldNode, x, commitNum);
 					oldParent = commitNum;
 					head = commitNum;
 					branches.put(curBranch, head);
 				}
 			}
 		}
 	}

 	private static void combineHistories(String fromBranch, String toBranch) {
 		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cale = Calendar.getInstance();
		String time = dateFormat.format(cale.getTime());

 		ArrayList<Integer> fromHist = branchHistory.get(fromBranch);
 		ArrayList<Integer> toHist = branchHistory.get(toBranch);
 		ArrayList<Integer> result = new ArrayList<Integer>();
 		result.addAll(fromHist);

 		Node oldNode = null;
 		Node newNode = null;
 		int oldParent = branches.get(fromBranch);

 		for (int x : toHist) {
 			if (!result.contains(x)) {
 				result.add(x);
 				oldNode = nodeTree.get(x);
 				newNode = new Node(oldNode, oldParent, time);
				commitNum += 1;
				nodeTree.put(commitNum, newNode);
				copyAll(oldNode, x, commitNum);
				oldParent = commitNum;
				head = commitNum;
				branches.put(curBranch, head);
 			}
 		}
 	}

 	private static int findSplit(String fromBranch, String toBranch) {
 		ArrayList<Integer> fromHist = branchHistory.get(fromBranch);
 		ArrayList<Integer> toHist = branchHistory.get(toBranch);
 		ArrayList<Integer> result = new ArrayList<Integer>();
 		result.addAll(toHist);
 		result.retainAll(fromHist);
 		if (toHist.get(toHist.size() - 1).equals(result.get(result.size() - 1))) {
 			return -1;
 		}
 		return result.get(result.size() - 1);
 	}

 	private static int findPostSplit(String fromBranch, String toBranch) {
 		int splitPoint = findSplit(fromBranch, toBranch);
 		ArrayList<Integer> fromHist = branchHistory.get(fromBranch);
 		ArrayList<Integer> toHist = branchHistory.get(toBranch);
 		ArrayList<Integer> result = new ArrayList<Integer>();
 		result.addAll(toHist);
 		result.retainAll(fromHist);
 		int firstInd = toHist.indexOf(splitPoint) + 1;
 		if (toHist.get(toHist.size() - 1) == result.get(result.size() - 1)) {
 			return -1;
 		} else {
 			return toHist.get(firstInd);
 		}
 	}

 	private static void initializeStructures() {

 		nodeTree = new HashMap<Integer, Node>();
		messages = new HashMap<String, Integer>();
		added = new HashSet<String>();
		removed = new HashSet<String>();
		unstaged = new HashSet<String>();
		branches = new HashMap<String, Integer>();
		branchHistory = new HashMap<String, ArrayList<Integer>>();
 	}

 	private static void writeObjects() {

 		serialize(nodeTree, ".gitlet/nodeTree.ser");
 		serialize(messages, ".gitlet/messages.ser");
 		serialize(added, ".gitlet/added.ser");
 		serialize(removed, ".gitlet/removed.ser");
 		serialize(unstaged, ".gitlet/unstaged.ser");
 		serialize(branches, ".gitlet/branches.ser");
 		serialize(branchHistory, ".gitlet/branchHistory.ser");
 		serialize(curBranch, ".gitlet/curBranch.ser");
 		serializeInt(head, ".gitlet/head.ser");
 		serializeInt(commitNum, ".gitlet/commitNum.ser");
 		serializeBool(initialized, ".gitlet/initialized.ser");
 	}

 	private static void serialize(Object o, String name) {
 		try {
 			FileOutputStream outFile = new FileOutputStream(name);
 			ObjectOutputStream outObject = new ObjectOutputStream(outFile);
 			outObject.writeObject(o);
 			outFile.close();
 			outObject.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}

 	private static void serializeInt(int integer, String name) {
 		try {
 			FileOutputStream outFile = new FileOutputStream(name);
 			DataOutputStream outObject = new DataOutputStream(outFile);
 			outObject.writeInt(integer);
 			outFile.close();
 			outObject.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}

 	private static void serializeBool(Boolean bool, String name) {
 		try {
 			FileOutputStream outFile = new FileOutputStream(name);
 			DataOutputStream outObject = new DataOutputStream(outFile);
 			outObject.writeBoolean(bool);
 			outObject.close();
 			outFile.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}

 	private static void readObjects() {

		nodeTree = (HashMap<Integer, Node>) deserialize(".gitlet/nodeTree.ser");
		messages = (HashMap<String, Integer>) deserialize(".gitlet/messages.ser");
		added = (HashSet<String>) deserialize(".gitlet/added.ser");
		removed = (HashSet<String>) deserialize(".gitlet/removed.ser");
		unstaged = (HashSet<String>) deserialize(".gitlet/unstaged.ser");
		branches = (HashMap<String, Integer>) deserialize(".gitlet/branches.ser");
		branchHistory = (HashMap<String, ArrayList<Integer>>) deserialize(".gitlet/branchHistory.ser");
		curBranch = (String) deserialize(".gitlet/curBranch.ser");
		head = deserializeInt(".gitlet/head.ser");
		commitNum = deserializeInt(".gitlet/commitNum.ser");
		initialized = deserializeBool(".gitlet/initialized.ser");

 	}

 	private static Object deserialize(String name) {
 		try {
 			FileInputStream inFile = new FileInputStream(name);
 			ObjectInputStream inObject = new ObjectInputStream(inFile);
 			Object outObject = inObject.readObject();
 			inObject.close();
 			inFile.close();
 			return outObject;

 		} catch (IOException e) {
 			e.printStackTrace();
 			return null;
 		} catch (ClassNotFoundException c) {
 			c.printStackTrace();
 			return null;
 		}
 	}

 	private static int deserializeInt(String name) {
 		try {
 			FileInputStream inFile = new FileInputStream(name);
 			DataInputStream inObject = new DataInputStream(inFile);
 			int outObject = inObject.readInt();
 			inObject.close();
 			inFile.close();
 			return outObject;

 		} catch (IOException e) {
 			e.printStackTrace();
 			return 0;
 		} 
 	}

 	private static boolean deserializeBool(String name) {
 		try {
 			FileInputStream inFile = new FileInputStream(name);
 			DataInputStream inObject = new DataInputStream(inFile);
 			boolean outObject = inObject.readBoolean();
 			inObject.close();
 			inFile.close();
 			return outObject;

 		} catch (IOException e) {
 			e.printStackTrace();
 			return false;
 		} 
 	}

 	private static boolean equalFile(String f1, String f2) {
 		String s1 = contentsToString(f1);
 		String s2 = contentsToString(f2);
 		if (s1.equals(s2)) {
 			return true;
 		}
 		return false;
 	}

 	private static String contentsToString(String f) {
 		try {
 			FileInputStream inFile = new FileInputStream(f);
	 		BufferedReader inRead = new BufferedReader(new InputStreamReader(inFile));
	 		StringBuilder builder = new StringBuilder();
	 		String nextLine = inRead.readLine();
	 		while (nextLine != null) {
	 			builder.append(nextLine);
	 			nextLine = inRead.readLine();
	 		}
	 		return builder.toString();
 		} catch (IOException e) {
 			return null;
 		}
 		
 	}

 	private static class Node implements Serializable {
 		private int parent;
 		private String commitMessage;
 		private HashSet<String> fileNames; 
 		private String time;
 		public Node(int parent0, String msg, HashSet<String> files, String time0) {
 			parent = parent0;
 			commitMessage = msg;
 			fileNames = files;
 			time = time0;
 		}

 		public Node(Node oldNode, int parent0, String time0) {
 			parent = parent0;
 			time = time0;
 			commitMessage = oldNode.commitMessage;
 			fileNames = new HashSet<String>();
 			fileNames.addAll(oldNode.fileNames);
 		}

 		public Node(Node oldNode, int parent0, String msg, String time0) {
 			parent = parent0;
 			time = time0;
 			commitMessage = msg;
 			fileNames = new HashSet<String>();
 			fileNames.addAll(oldNode.fileNames);
 		}
 	}
}
