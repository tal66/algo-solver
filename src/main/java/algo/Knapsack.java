package algo;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Knapsack implements AlgoSolver{
    private static final Logger logger = LoggerFactory.getLogger(Knapsack.class);

    private String filename;
    private List<Integer> values;
    private List<Integer> weights;
    private int C;

    public Knapsack() {
    }

    /**
     * @return item indices
     */
    public String solve(List<String> data, String filename){
        logger.info("solving file <{}>", filename);
        this.filename = filename;

        boolean success = parse(data);
        if (!success){
            return "";
        }

        int n = values.size();
        int[][] A = new int[n+1][C+1];

        for (int i=1; i <= n; i++){
            int item = i-1;
            for (int c=1; c <= C; c++){
                int withoutItem = A[i - 1][c];
                if (c - weights.get(item) < 0){
                    A[i][c] = withoutItem;
                } else {
                    int withItem = A[i - 1][c - weights.get(item)] + values.get(item);
                    A[i][c] = Math.max(withItem, withoutItem);
                }
            }
        }

        int c = C;
        List<Integer> items = new ArrayList<>();
        for (int i = n; i >= 1; i--){
            int item = i - 1;
            if (c - weights.get(item) < 0){
                continue;
            }
            if (A[i-1][c] < A[i-1][c - weights.get(item)] + values.get(item)){
                items.add(item);
                c -= weights.get(item);
            }
        }

        Collections.reverse(items);
        logger.info("finished solving file <{}>", filename);
        return Joiner.on(" ").join(items);
    }

    /**
     * @param data format: Knapsack, [values], [weights], [C]. e.g:
     *                 Knapsack
     *                 1 5 6
     *                 2 4 5
     *                 7
     */
    public boolean parse(List<String> data){
        try {
            values = Util.splitBySpacesToIntegers(data, 1);
            weights = Util.splitBySpacesToIntegers(data, 2);
            C = Util.getInt(data, 3);
        } catch (Exception e){
            logger.error("file {}: parsing error. {}", filename, e);
            return false;
        }

        if (values.size() != weights.size()){
            logger.error("file {}: values.size() != weights.size() [{} != {}]", filename, values.size(), weights.size());
            return false;
        }

        return true;
    }
}
