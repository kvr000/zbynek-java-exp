# Useful debugging tools and snippets

## Debug snippets

### Map to lines string

```
.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n"))
```
