package algo;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
    private static final Pattern spaces = Pattern.compile("\\s+");

    public static int getInt(List<String> data, int lineNum){
        return Integer.parseInt(Splitter.on(spaces).split(data.get(lineNum).strip())
                .iterator()
                .next());
    }

    public static List<String> splitBySpaces(List<String> data, int lineNum){
        return Splitter.on(spaces).splitToList(data.get(lineNum).strip());
    }

    public static List<Integer> splitBySpacesToIntegers(List<String> data, int lineNum){
        return Splitter.on(spaces).splitToList(data.get(lineNum).strip())
                .stream()
                .map(x -> Integer.parseInt(x))
                .collect(Collectors.toList());
    }
}
