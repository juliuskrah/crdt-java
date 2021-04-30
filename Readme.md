# State-Based LWW-Element-Graph CRDT

> Conflict Free Replicated Data Types (`CRDTs`) are data structures that power real time collaborative 
  applications in distributed systems. CRDTs can be replicated across systems, they can be updated independently 
  and concurrently without coordination between the replicas, and it is always mathematically possible to resolve 
  inconsistencies which might result.

The code in this repository illustrates an implementation of a CRDT graph. The implementation provided is by
no means intended to be efficient, performant, synchronised for concurrent access or even production ready. 
It is provided here as a guide to illustrate a narrow view of CRDT.

## Prerequisite

In order to run this sample, you'd need Java Development Kit (`JDK`) 11 and Apache Maven (Optional).

You can execute the tests with the following command

On Unix systems

```bash
> ./mvnw clean test
```

On Windows Powershell

```posh
PS C:\> .\mvnw clean test
```

## Graph

I will be using the directed acyclic graph below and implementation found [here](./src/main/java/com/juliuskrah/LWWElementGraph.java)

```
                 _______
                |       |
                | zumar |
          _     |_______|      _
          /\                  /\
         /                      \
        /                        \
       /                          \
      /                            \
     /                              \
 _______         _______         _______
|       |       |       |       |       |
| alice |<------| julius|------>| james |
|_______|       |_______|       |_______|
    \                               /
     \                             /
      \                           /
       \                         /
        \                       /
         \                     / 
         _\/     _______     \/_
                |       |
                | freda |
                |_______|
```

The adjacency list below presents the relationships:

```
[julius] -> [alice ] [james ] [      ]
[alice ] -> [freda ] [julius] [zumar ]
[freda ] -> [alice ] [james ] [      ]
[zumar ] -> [alice ] [james ] [      ]
[james ] -> [freda ] [julius] [zumar ]
```

## Working with Vertices

Test cases can be found [here](./src/test/java/com/juliuskrah/LWWElementGraphTest.java)

### `addVertex`

To add a new vertex:

```java
private final CRDTStoreFactory factory = CRDTStoreFactory.getInstance();
final CRDTStore crdtStore1 = factory.crdtStore("ND-41");
// create an LWW-Element-Graph and find a second replica from store
final var replica1 = crdtStore1.<String>createLWWElementGraph("15-AD");

SoftAssertions softly = new SoftAssertions();
replica1.addVertex("julius");
replica1.addVertex("freda");
replica1.addVertex("alice");
replica1.addVertex("zumar");
replica1.addVertex("james");
// 5 vertices were added
softly.assertThat(replica1.vertexSize()).isEqualTo(5);
softly.assertAll();
```

### `removeVertex`

To remove a vertex:

```java
LWWElementGraph replica1 = ...
SoftAssertions softly = new SoftAssertions();
replica1.removeVertex("alice");
// 1 vertex removed, 4 remaining
softly.assertThat(replica1.vertexSize()).isEqualTo(4);
softly.assertAll();
```

### `containsVertex`

### `findAdjacentVertices`

To query for all vertices connected to a vertex:

```java
LWWElementGraph replica1 = ...
SoftAssertions softly = new SoftAssertions();
// vertex [julius] has 2 adjacent vertices
softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty()
    .extracting(Vertex::getValue).containsOnly("james", "alice");
softly.assertAll();
```

### `findPath`

## Working with Edges

### `addEdge`

To add an edge:

```java
LWWElementGraph replica1 = ...
SoftAssertions softly = new SoftAssertions();
// you can only add an edge to existing vertices. Operation does not succeed
var success = replica1.addEdge("peter", "eunice");
softly.assertThat(success).isFalse();
// add edge incident on julius and james
result = replica1.addEdge("julius", "james");
softly.assertThat(result).isTrue();
softly.assertAll();
```

### `removeEdge`

To remove an edge:

```java
LWWElementGraph replica1 = ...
SoftAssertions softly = new SoftAssertions();
// remove edge incident on julius and james
replica1.removeEdge("julius", "james");
// only the edge incident on julius and alice remains
softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty()
    .extracting(Vertex::getValue).containsOnly("alice");
softly.assertAll();
```

## Working with Replicas

### `connect` / `merge`

## Test Coverage

After running `mvnw test` a coverage report is generated:
`jacoco` maven plugin is used to generate a [coverage report](./target/site/jacoco/index.html)
