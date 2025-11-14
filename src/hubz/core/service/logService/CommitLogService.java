package hubz.core.service.logService;

import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.service.RepositoryHelper;
import hubz.io.JsonSerializer;
import hubz.model.commitmodel.CommitModel;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommitLogService {

    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";

    private static final String RED     = "\u001B[31m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String BLUE    = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN    = "\u001B[36m";
    private static final String WHITE   = "\u001B[37m";

    //Used to load <limit or 25> commits from head
    public List<String> loadCommitLogs(int limit)
            throws IOException, RepositoryNotFoundException {

        List<String> output = new ArrayList<>();
        RepositoryHelper helper = new RepositoryHelper();
        // Traverse graph
        List<String> commitOrder = helper.traverseGraphFromHead();

        // Only load requested number of commits
        int count = Math.min(limit, commitOrder.size());
        String commitDir = new File(HubzContext.getRootDir(), HubzPath.COMMITS_DIR).getAbsolutePath();

        String headHash = helper.getHeadCommitHash();

        //Saving commit history in list
        for (int i = 0; i < count; i++) {
            String hash = commitOrder.get(i);
            File commitFile = new File(commitDir, hash + ".json");

            if (!commitFile.exists()) continue;

            CommitModel c = JsonSerializer.readJsonFile(commitFile, CommitModel.class);

            StringBuilder block = new StringBuilder();

            block.append(BOLD).append(CYAN)
                    .append("commit ").append(hash)
                    .append(RESET);

            if (hash.equals(headHash)) {
                block.append(" ")
                        .append(BOLD).append(BLUE)
                        .append("(HEAD -> ").append(c.getBranchName()).append(")")
                        .append(RESET);
            }

            block.append("\n");

            block.append(YELLOW)
                    .append("Number: ").append(c.getCommitNumber())
                    .append(RESET).append("\n");

            block.append(GREEN)
                    .append("Author: ").append(c.getAuthor())
                    .append(RESET).append("\n");

            block.append(MAGENTA)
                    .append("Date:   ").append(c.getTimestamp())
                    .append(RESET).append("\n");

            block.append(BOLD).append(WHITE)
                    .append("Message: ").append(c.getMessage())
                    .append(RESET).append("\n");

            block.append(CYAN)
                    .append("-------------------------------------")
                    .append(RESET).append("\n\n");

            output.add(block.toString());
        }

        return output;
    }
}
