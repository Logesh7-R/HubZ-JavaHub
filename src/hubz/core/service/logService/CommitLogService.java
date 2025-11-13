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
            block.append("Commit: ").append(hash);
            if (hash.equals(headHash)) {
                block.append(" (HEAD, ").append(c.getBranchName()).append(")");
            }
            block.append("\n");

            block.append("Number: ").append(c.getCommitNumber()).append("\n");
            block.append("Author: ").append(c.getAuthor()).append("\n");
            block.append("Date  : ").append(c.getTimestamp()).append("\n");
            block.append("Message: ").append(c.getMessage()).append("\n");

            output.add(block.toString());
        }

        return output;
    }
}
