package com.vanilla;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlParser {

    public Map<String, Object> parseYaml(String yamlString) {

        Map<String, Object> map = new HashMap<>();
        Map<String, Map<String, Object>> aliasKeysReference = new HashMap<>();

        List<LineWithIdentation> lineWithIdentations = Stream.of(yamlString.split("\n"))
                .map(LineWithIdentation::new)
                .toList();

        for (int i = 0; i < lineWithIdentations.size(); i++) {
            if (lineWithIdentations.get(i).hasValue() && !lineWithIdentations.get(i).isAlias) {
                int currentIdent = lineWithIdentations.get(i).ident;
                String completeKey = "";
                int k = i;
                Map<String, List<String>> aliasKeys = new HashMap<>();

                do {
                    LineWithIdentation line = lineWithIdentations.get(k);
                    int ident = line.ident;

                    if (ident < currentIdent) {
                        if (line.isAlias) {
                            List<String> orDefault = aliasKeys.computeIfAbsent(
                                    line.value, k1 -> new ArrayList<>());
                            orDefault.add(completeKey + "$");
                        }

                        currentIdent = ident;
                        completeKey = line.key + (completeKey.isBlank() ? "" : "." + completeKey);
                    }

                    k--;
                } while (k >= 0);

                String key = ("".equals(completeKey) ? "" : completeKey + ".") + lineWithIdentations.get(i).key;
                String linha = "";
                String groupedValue = "";
                Object value = parseValue(lineWithIdentations.get(i).value.trim());
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

    private static class LineWithIdentation {
        int ident;
        String key;
        String value;
        Boolean isAlias = false;
        Boolean isText = false;

        public LineWithIdentation(String value) {
            this.ident = (int) (value.chars().takeWhile(p -> p == ' ').count() / 2);
            String[] split = value.trim().split(":", 2);
            this.key = split[0];
            if (split.length > 1) {
                String trimmedValue = split[1].trim();
                this.isAlias = trimmedValue.startsWith("&");
                this.value = trimmedValue.substring(isAlias ? 1 : 0);
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
        return valueString.trim();
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
