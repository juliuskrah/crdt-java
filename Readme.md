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
// create an LWW-Element-Graph
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

To check if a vertex is in graph

```java
LWWElementGraph replica1 = ...
SoftAssertions softly = new SoftAssertions();
// julius is a vertex in graph
softly.assertThat(replica1.containsVertex("julius")).isTrue();
// duh
softly.assertThat(replica1.containsVertex("notfound")).isFalse();
softly.assertAll();
```

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

TODO

## Working with Edges

An `edge` is incident on vertices, so an `edge` cannot exist without a vertice

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

To simulate two replicas, I'd be using an [`InMemoryCRDTStore`](./src/main/java/com/juliuskrah/InMemoryCRDTStore.java).

### `connect` / `merge`

Create two replicas and update them concurrently or independently.

```java
// create two CRDT Stores and connect them
final CRDTStore crdtStore1 = factory.crdtStore("ND-41");
final CRDTStore crdtStore2 = factory.crdtStore("ND-42");
crdtStore1.connect(crdtStore2);

// create an LWW-Element-Graph and find a second replica from store
final var replica1 = crdtStore1.<String>createLWWElementGraph("15-AD");
final var replica2 = crdtStore2.<String>findLWWElementGraph("15-AD").get();

SoftAssertions softly = new SoftAssertions();
// 5 vertices [julius, james, zumar, alice, freda] added to either replica
replica1.addVertex("julius");
replica1.addVertex("james");
replica2.addVertex("zumar");
replica2.addVertex("alice");
replica2.addVertex("freda");
// 5 vertices were added across 2 replicas
softly.assertThat(replica1.vertexSize()).isEqualTo(5);
softly.assertThat(replica2.vertexSize()).isEqualTo(5);

// add edges [[alice,zumar], [james,zumar], [julius,alice], [alice,freda], [james,freda], [julius,james]]
// across 2 repicas
replica1.addEdge("alice", "zumar");
replica1.addEdge("james", "zumar");
replica2.addEdge("julius", "alice");
replica2.addEdge("alice", "freda");
replica2.addEdge("james", "freda");
replica1.addEdge("julius", "james");
// both replicas should see the update
softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty()
    .extracting(Vertex::getValue).containsOnly("james", "alice");
softly.assertThat(replica2.findAdjacentVertices("james")).isNotEmpty()
    .extracting(Vertex::getValue).containsOnly("julius", "freda", "zumar");

// disconnect the stores simulating a network issue, brain split
crdtStore1.disconnect(crdtStore2);

// replica2 is updated while in a disconnected state
replica2.addEdge("julius", "james");
softly.assertThat(replica2.findAdjacentVertices("julius")).isNotEmpty()
    .extracting(Vertex::getValue).containsOnly("james", "alice");
// replica1 hasn't seen the update yet
softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty() //	
    .extracting(Vertex::getValue).containsOnly("alice");

// reconnect the stores
crdtStore1.connect(crdtStore2);
// replica 2 is synchronised now
softly.assertThat(replica1.findAdjacentVertices("julius")).isNotEmpty() //	
    .extracting(Vertex::getValue).containsOnly("alice", "james");

softly.assertAll();
```

## Test Coverage

After running `mvnw test` a coverage report is generated:
`jacoco` maven plugin is used to generate a [coverage report](./target/site/jacoco/index.html)
