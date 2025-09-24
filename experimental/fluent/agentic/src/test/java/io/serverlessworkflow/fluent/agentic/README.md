# Implementation of Scenarios from [LangChain4j Agents Tutorials](https://docs.langchain4j.dev/tutorials/agents/) for CNCF Workflow Java DSL

# Sequential workflow
### Common part:
```java
public interface AudienceEditor {

    @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the target audience of {{audience}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
    @Agent("Edits a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
}

interface CreativeWriter {

  @UserMessage("""
        You are a creative writer.
        Generate a draft of a story no more than
        3 sentences long around the given topic.
        Return only the story and nothing else.
        The topic is {{topic}}.
        """)
  @Agent("Generates a story based on the given topic")
  String generateStory(@V("topic") String topic);
}

public interface StyleEditor {

  @UserMessage("""
          You are a professional editor.
          Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
          Return only the story and nothing else.
          The story is "{{story}}".
          """)
  @Agent("Edits a story to better fit a given style")
  String editStory(@V("story") String story, @V("style") String style);
}

CreativeWriter creativeWriter = AgenticServices
        .agentBuilder(CreativeWriter.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

AudienceEditor audienceEditor = AgenticServices
        .agentBuilder(AudienceEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

StyleEditor styleEditor = AgenticServices
        .agentBuilder(StyleEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

Map<String, Object> input = Map.of(
        "topic", "dragons and wizards",
        "style", "fantasy",
        "audience", "young adults"
);
```

### LangChain4j
```java
UntypedAgent novelCreator = AgenticServices
        .sequenceBuilder()
        .subAgents(creativeWriter, audienceEditor, styleEditor)
        .outputName("story")
        .build();

String story = (String) novelCreator.invoke(input);
```

### Serverless Workflow

```java
Workflow wf = workflow("seqFlow").tasks(tasks -> tasks.sequence("process", creativeWriter, audienceEditor, styleEditor)).build();

try (WorkflowApplication app = WorkflowApplication.builder().build()) {
    String result = app.workflowDefinition(wf).instance(input).start().get().asText().orElseThrow();
} catch (Exception e) {
    throw new RuntimeException("Workflow execution failed", e);
}
```


### Loop workflow
### Common part:
```java
  interface StyleEditor {

  @UserMessage(
          """
                  You are a professional editor.
                  Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
                  Return only the story and nothing else.
                  The story is "{{story}}".
                  """)
  @Agent("Edits a story to better fit a given style")
  String editStory(@V("story") String story, @V("style") String style);
}

interface StyleScorer {

  @UserMessage(
          """
                  You are a critical reviewer.
                  Give a review score between 0.0 and 1.0 for the following
                  story based on how well it aligns with the style '{{style}}'.
                  Return only the score and nothing else.

                  The story is: "{{story}}"
                  """)
  @Agent("Scores a story based on how well it aligns with a given style")
  double scoreStyle(@V("story") String story, @V("style") String style);
}

StyleEditor styleEditor = AgenticServices
        .agentBuilder(StyleEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

StyleScorer styleScorer = AgenticServices
        .agentBuilder(StyleScorer.class)
        .chatModel(BASE_MODEL)
        .outputName("score")
        .build();


```

### LangChain4j
```java
StyledWriter styledWriter = AgenticServices
        .sequenceBuilder(StyledWriter.class)
        .subAgents(creativeWriter, styleReviewLoop)
        .outputName("story")
        .build();

String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
```

### Serverless Workflow
```java
Predicate<AgenticScope> until = s -> s.readState("score", 0).doubleValue() >= 0.8;

Workflow wf =
        AgentWorkflowBuilder.workflow("retryFlow")
                .tasks(loop(until, scorer, 5, editor))
                .build();

Map<String, Object> input =
        Map.of(
                "story", "dragons and wizards",
                "style", "comedy");

try (WorkflowApplication app = WorkflowApplication.builder().build()) {
  String result = app.workflowDefinition(wf).instance(input).start().get().asText().orElseThrow();
} catch (Exception e) {
  throw new RuntimeException("Workflow execution failed", e);
}
```

### Parallel workflow
### Common part:
```java
public interface FoodExpert {

    @UserMessage("""
        You are a great evening planner.
        Propose a list of 3 meals matching the given mood.
        The mood is {{mood}}.
        For each meal, just give the name of the meal.
        Provide a list with the 3 items and nothing else.
        """)
    @Agent
    List<String> findMeal(@V("mood") String mood);
}

public interface MovieExpert {

    @UserMessage("""
        You are a great evening planner.
        Propose a list of 3 movies matching the given mood.
        The mood is {mood}.
        Provide a list with the 3 items and nothing else.
        """)
    @Agent
    List<String> findMovie(@V("mood") String mood);
}

FoodExpert foodExpert = AgenticServices
        .agentBuilder(FoodExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("meals")
        .build();

MovieExpert movieExpert = AgenticServices
        .agentBuilder(MovieExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("movies")
        .build();
```

### LangChain4j
```java
EveningPlannerAgent eveningPlannerAgent = AgenticServices
        .parallelBuilder(EveningPlannerAgent.class)
        .subAgents(foodExpert, movieExpert)
        .executor(Executors.newFixedThreadPool(2))
        .outputName("plans")
        .output(agenticScope -> {
            List<String> movies = agenticScope.readState("movies", List.of());
            List<String> meals = agenticScope.readState("meals", List.of());

            List<EveningPlan> moviesAndMeals = new ArrayList<>();
            for (int i = 0; i < movies.size(); i++) {
                if (i >= meals.size()) {
                    break;
                }
                moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return moviesAndMeals;
        })
        .build();

List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");
```

### Serverless Workflow
```java
Workflow wf = workflow("forkFlow").parallel("fanout", foodExpert, movieExpert).build();

Map<String, Object> input = Map.of("mood", "I am hungry and bored");

Map<String, Object> result;
try (WorkflowApplication app = WorkflowApplication.builder().build()) {
    result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
} catch (Exception e) {
   throw new RuntimeException("Workflow execution failed", e);
}
```

### Error handling
### Common part:
```java

```

### LangChain4j
```java

```

### Serverless Workflow

```java

```

### Human-in-the-loop
### Common part:
```java
public record HumanInTheLoop(Consumer<String> requestWriter, Supplier<String> responseReader) {

    @Agent("An agent that asks the user for missing information")
    public String askUser(String request) {
        requestWriter.accept(request);
        return responseReader.get();
    }
}

public interface AstrologyAgent {
  @SystemMessage("""
          You are an astrologist that generates horoscopes based on the user's name and zodiac sign.
          """)
  @UserMessage("""
          Generate the horoscope for {{name}} who is a {{sign}}.
          """)
  @Agent("An astrologist that generates horoscopes based on the user's name and zodiac sign.")
  String horoscope(@V("name") String name, @V("sign") String sign);
}

AstrologyAgent astrologyAgent = AgenticServices
        .agentBuilder(AstrologyAgent.class)
        .chatModel(BASE_MODEL)
        .build();

HumanInTheLoop humanInTheLoop = AgenticServices
        .humanInTheLoopBuilder()
        .description("An agent that asks the zodiac sign of the user")
        .outputName("sign")
        .requestWriter(request -> {
          System.out.println(request);
          System.out.print("> ");
        })
        .responseReader(() -> System.console().readLine())
        .build();
```

### LangChain4j
```java
SupervisorAgent horoscopeAgent = AgenticServices
        .supervisorBuilder()
        .chatModel(PLANNER_MODEL)
        .subAgents(astrologyAgent, humanInTheLoop)
        .build();

horoscopeAgent.invoke("My name is Mario. What is my horoscope?")
```

### Serverless Workflow

```java
Workflow wf = workflow("seqFlow").sequence("process", astrologyAgent, humanInTheLoop).build();

Map<String, Object> input = Map.of("request", "My name is Mario. What is my horoscope?");

try (WorkflowApplication app = WorkflowApplication.builder().build()) {
  String result = app.workflowDefinition(wf).instance(input).start().get().asMap().orElseThrow();
} catch (Exception e) {
  throw new RuntimeException("Workflow execution failed", e);
}
```