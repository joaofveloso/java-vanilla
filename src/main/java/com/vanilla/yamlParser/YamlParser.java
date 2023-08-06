package com.vanilla.yamlParser;

import com.vanilla.yamlParser.factory.Tuple;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlParser {

    private final Pattern keyValuePattern = Pattern.compile("(.*):\\s*(.*)");

    public Map<String, Object> parseYaml_(String yamlString) {

        Iterator<String> iterator = Stream.of(yamlString.split("\n")).iterator();
        return this.parseYaml_(iterator,new HashMap<>(), null);
    }

    public Map<String, Object> parseYaml_(Iterator<String> iterator, Map<String, Object> map, String key) {

        String value = iterator.next();
        Tuple<String, Object> t = null;
        if ((t = parseKeyValuePattern(value)) != null) {
            map.put(t.first(), t.second());
        }
        if (iterator.hasNext()) {
            return this.parseYaml_(iterator, map, key);
        }
        return map;
    }

    private Tuple<String, Object> parseKeyValuePattern(String value) {
        Matcher matcher = keyValuePattern.matcher(value);
        if (matcher.find()) {
            return new Tuple<>(matcher.group(1), matcher.group(2));
        }
        return null;
    }

    public Map<String, Object> parseYaml(String yamlString) {

        Map<String, Object> map = new HashMap<>();
        Map<String, Map<String, Object>> aliasKeysReference = new HashMap<>();

        List<LineWithIdentation> lineWithIdentations = Stream.of(yamlString.split("\n"))
                .map(LineWithIdentation::new)
                .toList();

        for (int i = 0; i < lineWithIdentations.size(); i++) {
            if (lineWithIdentations.get(i).hasValue() && !lineWithIdentations.get(i).isAlias) {
                int currentIdent = lineWithIdentations.get(i).indent;
                int k = i;
                Map<String, List<String>> aliasKeys = new HashMap<>();

                String completeKey = extractCompleteKey(aliasKeysReference, lineWithIdentations, currentIdent, k, aliasKeys);

                String key = ("".equals(completeKey) ? "" : completeKey + ".") + lineWithIdentations.get(i).key;
                String linha = "";
                String groupedValue = "";
                Object value = parseValue(lineWithIdentations.get(i).value.trim());
                if ("".equals(value) && lineWithIdentations.get(i + 1).indent > lineWithIdentations.get(i).indent) {
                    continue;
                }
                if (List.of("|", ">").contains(lineWithIdentations.get(i).value)) {
                    if ("|".equals(lineWithIdentations.get(i).value)) {
                        linha = "\n";
                    } else {
                        linha = " ";
                    }
                    do {
                        ++i;
                        groupedValue += (groupedValue.isBlank() ? "" : linha) + lineWithIdentations.get(i).key;
                    } while( lineWithIdentations.size()-1 > i && lineWithIdentations.get(i).isText);
                    map.put(key, groupedValue);
                } else if ("<<".equals(lineWithIdentations.get(i).key)) {
                    Map<String, Object> stringObjectMap = aliasKeysReference.get(
                            lineWithIdentations.get(i).value.trim().replace("*", ""));
                    String finalCompleteKey = completeKey;
                    stringObjectMap.forEach((a, b) -> map.put(finalCompleteKey + "." + a, b));
                } else if (String.valueOf(value).startsWith("*")) {
                    String aliasKey = String.valueOf(value).replace("*", "");
                    map.put(key, parseValue(aliasKeysReference.get(aliasKey).get(aliasKey).toString()));
                } else {
                    map.put(key, value);
                }

                int finalI = i;
                aliasKeys.forEach((a, b) -> {
                    b.stream().filter(p -> p.endsWith("$")).forEach(c -> {
                        String newKey = c.replace("$", "");
                        newKey = ("".equals(newKey) ? "" : newKey + ".") + lineWithIdentations.get(
                                finalI).key;
                        Map<String, Object> orDefault = aliasKeysReference.computeIfAbsent(a,
                                k1 -> new HashMap<>());
                        orDefault.put(newKey, value);
                    });
                });
            }
        }

        return map;
    }

    private static String extractCompleteKey(Map<String, Map<String, Object>> aliasKeysReference, List<LineWithIdentation> lineWithIdentations, int currentIdent, int k, Map<String, List<String>> aliasKeys) {
        String completeKey = "";
        do {
            LineWithIdentation line = lineWithIdentations.get(k);
            int ident = line.indent;

            if (ident < currentIdent) {
                if (line.isAlias) {
                    List<String> orDefault = aliasKeys.computeIfAbsent(
                            line.aliasKey, k1 -> new ArrayList<>());
                    orDefault.add(completeKey + "$");
                }
                currentIdent = ident;
                completeKey = line.key + (completeKey.isBlank() ? "" : "." + completeKey);
            }
            //TODO extrair
            if (line.rowAlias) {
                aliasKeysReference.put(line.aliasKey, Map.of(line.aliasKey, line.value));
            }

            k--;
        } while (k >= 0);
        return completeKey;
    }

    private static class LineWithIdentation {
        int indent;
        String key;
        String aliasKey;
        String value;
        Boolean isAlias = false;
        Boolean rowAlias = false;
        Boolean isText = false;

        public LineWithIdentation(String value) {
            this.indent = (int) (value.chars().takeWhile(p -> p == ' ').count() / 2);
            String[] split = value.trim().split(":", 2);
            this.key = split[0];
            if (split.length > 1) {
                String trimmedValue = split[1].trim();
                Boolean startWithAlias = trimmedValue.startsWith("&");
                if (startWithAlias) {
                    this.rowAlias = trimmedValue.split(" ").length > 1;
                    this.isAlias = !this.rowAlias;
                    this.aliasKey = trimmedValue.substring(1, trimmedValue.split(" ")[0].length());
                }
                this.value = trimmedValue.substring(isAlias || rowAlias ? trimmedValue.split(" ")[0].length() : 0).trim();
            }
            this.isText = !value.contains(":");
        }

        public boolean hasValue() {
            return Objects.nonNull(value);
        }
    }

    private Object parseValue(String valueString) {
        valueString = valueString.split("#")[0];
        if (valueString.equalsIgnoreCase("null")) {
            return null;
        }
        if (valueString.equalsIgnoreCase("true")) {
            return true;
        }
        if (valueString.equalsIgnoreCase("false")) {
            return false;
        }
        if (valueString.matches("-?\\d+")) {
            return Integer.parseInt(valueString);
        }
        if (valueString.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(valueString);
        }
        if ((valueString.startsWith("'") && valueString.endsWith("'")) ||
                (valueString.startsWith("\"") && valueString.endsWith("\""))) {
            return valueString.substring(1, valueString.length() - 1);
        }
        if (valueString.startsWith("{") && valueString.endsWith("}")) {
            return parseMap(valueString);
        }
        if (valueString.startsWith("[") & valueString.endsWith("]")) {
            return parseList(valueString);
        }
        if (valueString.startsWith("!!")) {

            return parceSplitTyping(valueString);
        }
        return valueString.trim();
    }

    private Object parceSplitTyping(String valueString) {
        if (valueString.startsWith("!!")) {
            return parceSplitTyping(valueString.substring(2, valueString.length()));
        }
        String[] s = valueString.split(" ");
        if ("float".equals(s[0])) {
            return Float.parseFloat(s[1]);
        }
        if ("timestamp".equals(s[0])) {
            return LocalDateTime.parse(s[1]);
        }
        return valueString;
    }

    private List<Object> parseList(String valueString) {
        if (valueString.startsWith("[") && valueString.endsWith("]")) {
            return parseList(valueString.substring(1, valueString.length() - 1));
        }
        return Stream.of(valueString.split(",")).map(String::trim).map(this::parseValue).collect(
                Collectors.toList());
    }

    public Map<String, Object> parseMap(String input) {
        return parseMapHelper(input, new HashMap<>());
    }

    private Map<String, Object> parseMapHelper(String input, Map<String, Object> map) {
        if (input == null || input.isEmpty() || input.isBlank()) {
            return map;
        }
        input = input.trim();
        if (input.charAt(0) == '{' && input.charAt(input.length() - 1) == '}') {
            input = input.substring(1, input.length() - 1);
        }
        String[] splitComma = input.split(",", 2);
        String[] split = splitComma[0].split(":", 2);
        String key = split[0].trim();
        String value = split.length > 1 ? split[1].trim() : "";
        if (value.startsWith("{")) {
            int indexOCurly = input.indexOf("{");
            int indexCCurly = input.indexOf("}");
            Map<String, Object> nestedMap = new HashMap<>();
            parseMapHelper(input.substring(indexOCurly, indexCCurly+1), nestedMap);
            map.put(key, nestedMap);
            return parseMapHelper(input.substring(indexCCurly + 1), map);
        } else {
            map.put(key, parseValue(value));
        }
        return splitComma.length > 1 ? parseMapHelper(splitComma[1], map) : map;
    }
}
