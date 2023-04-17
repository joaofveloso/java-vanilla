import com.vanilla.YamlParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlParserTest {

    public static void main(String[] args)
            throws InvocationTargetException, IllegalAccessException {

        Class<? extends YamlParserTest> aClass = YamlParserTest.class;
        Method[] declaredMethods = aClass.getDeclaredMethods();

        YamlParserTest test = new YamlParserTest(new YamlParser());
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.isAnnotationPresent(Test.class)) {
                declaredMethod.invoke(test);
            }
        }
    }

    private final YamlParser yamlParser;

    private YamlParserTest(YamlParser yamlParser) {
        this.yamlParser = yamlParser;
    }

    @Test
    void testBasicKeyValues() {
        String value = """
                key1: value1
                key2: value2
                key3: value3
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert "value1".equals(map.get("key1")) : "Expected 'value1', but got " + map.get("key1");
        assert "value2".equals(map.get("key2")) : "Expected 'value2', but got " + map.get("key2");
        assert "value3".equals(map.get("key3")) : "Expected 'value3', but got " + map.get("key3");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testNestedKeyValuePairs() {

        String value = """
                key1:
                  subkey1: value1
                  subkey2: value2
                  subkey3: value3 
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert "value1".equals(map.get("key1.subkey1")) : "Expected 'value1', but got " + map.get("key1.subkey1");
        assert "value2".equals(map.get("key1.subkey2")) : "Expected 'value2', but got " + map.get("key1.subkey2");
        assert "value3".equals(map.get("key1.subkey3")) : "Expected 'value3', but got " + map.get("key1.subkey3");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testMixingDataType() {
        String value = """
                string: "This is a string"
                integer: 42
                double: 3.14159
                boolean: true
                boolean2: false
                null_value: null
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert String.class.equals(map.get("string").getClass()) : "Expected 'String', but got " + map.get("string").getClass().getSimpleName();
        assert Integer.class.equals(map.get("integer").getClass()) : "Expected 'Integer', but got " + map.get("integer").getClass().getSimpleName();
        assert Double.class.equals(map.get("double").getClass()) : "Expected 'Double', but got " + map.get("double").getClass().getSimpleName();
        assert Boolean.class.equals(map.get("boolean").getClass()) : "Expected 'Boolean', but got " + map.get("boolean").getClass().getSimpleName();
        assert map.get("null_value") == null : "Expected 'Null', but got " + map.get("null_value");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    public void testAnchorAndAliases() {

        String value = """
                parent:
                  child1: &alias1
                    key1: value1
                    key2: value2
                  child2:
                    <<: *alias1
                    key3: value3
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert "value1".equals(map.get("parent.child1.key1")) : "Expected 'value1', but got " + map.get("parent.child1.key1");
        assert "value2".equals(map.get("parent.child1.key2")) : "Expected 'value2', but got " + map.get("parent.child1.key2");
        assert "value1".equals(map.get("parent.child2.key1")) : "Expected 'value1', but got " + map.get("parent.child2.key1");
        assert "value2".equals(map.get("parent.child2.key2")) : "Expected 'value2', but got " + map.get("parent.child2.key2");
        assert "value3".equals(map.get("parent.child2.key3")) : "Expected 'value3', but got " + map.get("parent.child2.key3");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testInlineMap() {
        String value = """
                person: { name: John Doe, age: 30, address: { street: 123 Main St, city: Anytown, state: CA } }
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);
        Map<String, Object> person = (Map<String, Object>) map.get("person");

        assert "John Doe".equals(person.get("name")) : "Expected 'John Doe', but got " + person.get("name");
        assert Integer.valueOf(30).equals(person.get("age")) : "Expected '30', but got " + person.get("age");

        Map<String, Object> address = (Map<String, Object>) person.get("address");

        assert "123 Main St".equals(address.get("street")) : "Expected '123 Main St', but got " + address.get("street");
        assert "Anytown".equals(address.get("city")) : "Expected 'Anytown', but got " + address.get("city");
        assert "CA".equals(address.get("state")) : "Expected 'CA', but got " + address.get("state");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testComments() {
        String value = """
                # This is a comment in a YAML file
                key1: value1  # This is a comment at the end of a line
                # This is a comment after the line
                key2: value2
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert "value1".equals(map.get("key1")) : "Expected 'value1', but got " + map.get("key1");
        assert "value2".equals(map.get("key2")) : "Expected 'value2', but got " + map.get("key2");

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testInlineList() {
        String value = """
                key1: [item1, item2, item3, item4]
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);
        Object list = map.get("key1");

        assert ArrayList.class.equals(list.getClass()) : "Expected 'ArrayList.class', but got " + map.get("key1").getClass().getSimpleName() ;
        assert Integer.valueOf(4).equals(((List) list).size()) : "Expected '4', but got " + ((List) list).size();

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testMultiLineString() {
        String value = """
                description: |
                  This is a multi-line string.
                  It can contain multiple lines of text.
                """;

        String result = """
                This is a multi-line string.
                It can contain multiple lines of text.""";

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert result.equals(map.get("description")): "Expected '" + result + "', but got '" + map.get("description") + "'";

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testFoldedStrings() {
        String value = """
                description: >
                  This is a folded string.
                  It can contain multiple lines of text,
                  but they are folded into a single line
                  when parsed by a YAML parser.
                """;

        String result = """
              This is a folded string. It can contain multiple lines of text, but they are folded into a single line when parsed by a YAML parser.""";

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert result.equals(map.get("description")): "Expected '" + result + "', but got '" + map.get("description") + "'";

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testExplicityTyping() {
        String value = """
                number: !!float 3.14159
                date: !!timestamp 2023-04-14T12:00:00
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert Float.class.equals(map.get("number").getClass()) : "Expected '" + Float.class.getSimpleName() + "', but got '" + map.get("number").getClass() + "'";
        assert LocalDateTime.class.equals(map.get("date").getClass()) : "Expected '" + LocalDateTime.class.getSimpleName() + "', but got '" + map.get("date").getClass() + "'";

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    @Test
    void testNestedItems() {
        String value = """
                person:
                  name: John Doe
                  age: 30
                  address:
                    street: 123 Main St
                    city: Anytown
                    state: CA
                """;

        Map<String, Object> map = yamlParser.parseYaml(value);

        assert "John Doe".equals(map.get("person.name")): "Expected 'John Does', but got '" + map.get("person.name")+ "'";
        assert Integer.valueOf(30).equals(map.get("person.age")): "Expected '30', but got '" + map.get("person.age")+ "'";
        assert "123 Main St".equals(map.get("person.address.street")): "Expected '123 Main St', but got '" + map.get("person.address.street")+ "'";
        assert "Anytown".equals(map.get("person.address.city")): "Expected 'Anytown', but got '" + map.get("person.address.city")+ "'";
        assert "CA".equals(map.get("person.address.state")): "Expected 'CA', but got '" + map.get("person.address.state")+ "'";

        String methodName = new Exception().getStackTrace()[0].getMethodName();
        System.out.println(methodName + ": SUCCESS");
    }

    void testAlisesAndAchorsForNestedRawValues() {
        String value = """
                parent:
                  child1:
                    key1: &alias1 value1
                    key2: value2
                  child2:
                    key3: *alias1
                    key4: value4
                """;
    }

    void testAliasesAndAchordWithList() {
        String value = """
                list1: &mylist [1, 2, 3]
                list2: *mylist
                """;
    }

    void testEscapedChars() {
        String value = """
                string: "This is a string with \\"escaped\\" characters"
                """;
    }

    void testNullValues() {
        String value = """
                string: "This is a string with \\"escaped\\" characters"
                """;
    }

    void testFetchDataFromVmParams() {
        String value = """
                string: "This value '${core.hide.value:Not Found}' comes from VM"
                """;
    }

    void testBinaryData() {
        String value = """
                image: !!binary |
                  R0lGODlhAQABAIAAAAUEBAAAACwAAAAAAQABAAACAkQBADs=
                """;
    }

    void testUnorderedSet() {
        String value = """
                fruits: !!set
                  ? apple
                  ? banana
                  ? cherry
                """;
    }

    void testTaggedValues() {
        String value = """
                !myapp/CustomType
                  field1: value1
                  field2: value2           
                """;
    }


}
