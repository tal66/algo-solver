package algo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class SequenceAlignment implements AlgoSolver{
    private static final Logger logger = LoggerFactory.getLogger(SequenceAlignment.class);

    private String filename;
    private int gapPenalty;
    private int noMatchPenalty;
    private List<String> str1;
    private List<String> str2;

    public SequenceAlignment() {
    }

    @Override
    public String solve(List<String> data, String filename) {
        logger.info("solving file <{}>", filename);
        boolean success = parse(data);
        if (!success){
            return "";
        }

        this.filename = filename;
        int m = str1.size();
        int n = str2.size();

        int[][] A = new int[m+1][n+1];
        for (int i=0; i <= m; i++){
            A[i][0] = i * gapPenalty;
        }
        for (int i=0; i <= n; i++){
            A[0][i] = i * gapPenalty;
        }

        for (int i=1; i <= m; i++){
            String c1 = str1.get(i-1);
            for (int j=1; j <= n; j++){
                String c2 = str2.get(j-1);
                int match = c1.equals(c2) ? 0 : noMatchPenalty;
                A[i][j] = Math.min(A[i][j-1] + gapPenalty, A[i-1][j] + gapPenalty);
                A[i][j] = Math.min(A[i-1][j-1] + match, A[i][j]);
            }
        }
        int cost = A[m][n];

        //

        int i = m;
        int j = n;

        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        String gap = "-";
        while (i > 0 || j > 0) {
            if (i <= 0) {
                sb1.append(gap);
                sb2.append(str2.get(j - 1));
                j--;
                continue;
            } else if (j <= 0) {
                sb1.append(str1.get(i - 1));
                sb2.append(gap);
                i--;
                continue;
            }

            String c1 = str1.get(i - 1);
            String c2 = str2.get(j - 1);
            int v1 = A[i - 1][j - 1];
            int v2 = A[i - 1][j];
            int v3 = A[i][j - 1];

            if (v1 <= Math.min(v2, v3)) {
                sb1.append(c1);
                sb2.append(c2);
                i--;
                j--;
            } else if (v2 < Math.min(v1, v3)) {
                sb1.append(c1);
                sb2.append(gap);
                i--;
            } else {
                sb1.append(gap);
                sb2.append(c2);
                j--;
            }
        }

        logger.info("finished solving file <{}>", filename);
        return sb1.append("\n").append(sb2).reverse().append("\n").append(cost).toString();
    }


    /**
     * @param data format: SequenceAlignment, [str1], [str2], [gap], [mismatch]. e.g:
     *                 SequenceAlignment
     *                 A B C
     *                 A V A
     *                 2
     *                 1
     */
    @Override
    public boolean parse(List<String> data){
        try {
            str1 = Util.splitBySpaces(data, 1);
            str2 = Util.splitBySpaces(data, 2);
            logger.debug("str1 length = {},  str2 length = {}", str1.size(), str2.size());

            gapPenalty = Util.getInt(data, 3);
            noMatchPenalty = Util.getInt(data, 4);
            logger.debug("gapPenalty = {},  noMatchPenalty = {}", gapPenalty, noMatchPenalty);
        } catch (Exception e){
            logger.error("file {}: parsing error. {}", filename, e);
            return false;
        }

        return true;
    }
}
