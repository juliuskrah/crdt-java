# State-Based LWW-Element-Graph CRDT

> Conflict Free Replicated Data Types (`CRDTs`) are data structures that power real time collaborative 
  applications in distributed systems. CRDTs can be replicated across systems, they can be updated independently 
  and concurrently without coordination between the replicas, and it is always mathematically possible to resolve 
  inconsistencies which might result.

The code in this repository illustrates an implementation of a CRDT graph. The implementation provided is by
no means intended to be efficient, performant or production ready. It is provided here as a guide to illustrate
a narrow view of CRDT.

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

## Working with Vertices

### `addVertex`

### `removeVertex`

### `containsVertex`

### `findAllConnectedVertices`

### `findPath`

## Working with Edges

### `addEdge`

### `removeEdge`

## Working with Replicas

### `connect` / `merge`

## Coverage

After running `mvnw test` a coverage report is generated:
`jacoco` maven plugin is used to generate a [coverage report](./target/site/jacoco/index.html)
