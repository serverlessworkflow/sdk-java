# Workflow Validations

| Description                                 | Implemented |
|---------------------------------------------|-------------|
| `id` must be present and not be empty.      | Yes         |
| `version` must be present and not be empty. | Yes         |
| `start` must refer to an existing `state`.  | Yes         |
| `states` must be present and not be empty.  | Yes         |


## Functions

| Description                                                    | Implemented |
|----------------------------------------------------------------|-------------|
| `operation` must be present.                                   | No          |

## States

| Description                              | Implemented |
|------------------------------------------|-------------|
| `name` must be present and not be empty. | Yes         |


### Switch State

| Description                                                    | Implemented |
|----------------------------------------------------------------|-------------|
| `dataCondition` or `eventCondition` must be present.           | Yes         |
| Both `dataCondition` and `eventCondition` must not be present. | Yes         |
| `defaultCondition` must not be present.                        | Yes         |

#### Default Condition

| Description                                    | Implemented |
|------------------------------------------------|-------------|
| `nextState` or `end` must be present.          | Yes         |
| `nextState` must refer to an existing `state`. | Yes         |

#### Event Condition

| Description                                | Implemented |
|--------------------------------------------|-------------|
| Event or workflow timeout must be present. | Yes         |

#### Timeouts

| Description                                       | Implemented |
|---------------------------------------------------|-------------|
| `eventTimeout` must be a valid ISO 8601 duration. | Yes         |