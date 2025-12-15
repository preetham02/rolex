# Supported PromQL (Subset) in `promserver`

This project implements a **small PromQL subset** via JavaCC parsers:

- `src/main/javacc/GeneratedPromQLParser.jj` (standard instant-vector expressions)
- `src/main/javacc/GroupPromQLParser.jj` (custom `*_by[...]` and `group_by[...]` extensions)

If a query is outside the documented subset below, it may fail to parse or may not compile to SQL correctly.

---

## Supported syntax (standard PromQL subset)

### Tokens
- **identifier**: `[A-Za-z_][A-Za-z0-9_]*`
- **number**: `123` or `123.45`
- **string**: `"text"` or `'text'`
- **duration**: `<number><unit>` where unit is one of `s|m|h|d|w|y` (examples: `5m`, `1h`, `1y`)

### Label filters (matchers)

Inside `{ ... }` the following matchers are supported:
- `label="value"` / `label='value'`
- `label!="value"` / `label!='value'`

Not supported: `=~`, `!~`, `<`, `>`, regexes, set matchers, etc.

### Metric selector

```
metric_selector :=
  metric_name [ "{" label_filters "}" ]

label_filters :=
  label_filter { "," label_filter }

label_filter :=
  IDENT ( "=" | "!=" ) STRING
```

Examples:
- `sales`
- `sales{platform="mobile"}`
- `sales{platform!="tablet",region="us"}`

### Arithmetic expressions

Supported operators: `+ - * /` with standard precedence (`*`/`/` bind tighter than `+`/`-`).

```
expr := add_expr
add_expr := mul_expr { ("+" | "-") mul_expr }
mul_expr := term { ("*" | "/") term }
term := function_call | metric_selector | "(" expr ")"
```

### Range vector

Range selectors are supported for:
- `metric[5m]`
- `(expr)[5m]` (range applied to a parenthesized expression)

```
range_vector := term "[" DURATION "]"
```

Notes:
- This is **not full PromQL subquery support** (no `[range:step]`).

### Functions

#### `_over_time` functions (require a range vector argument)

Supported:
- `sum_over_time(range_vector)`
- `count_over_time(range_vector)`
- `min_over_time(range_vector)`
- `max_over_time(range_vector)`
- `avg_over_time(range_vector)`
- `stddev_over_time(range_vector)`
- `stddev_samp_over_time(range_vector)`

Examples:
- `avg_over_time(sales[1h])`
- `avg_over_time((nifty * gold)[1h])`
- `stddev_over_time(sales{platform!="tablet"}[1y])`

#### Scalar aggregation functions (used only in scalar contexts)

Supported:
- `sum(expr)`
- `count(expr)`
- `min(expr)`
- `max(expr)`
- `avg(expr)`
- `stddev_samp(expr)`

#### “Singular” functions (instant-vector → instant-vector)

Supported:
- `abs`, `acos`, `asin`, `atan`, `atan2`, `ceil`, `cos`, `cosh`, `degrees`, `e`, `exp`,
  `floor`, `ln`, `log`, `pi`, `power`, `radians`, `round`, `sign`, `sin`, `sinh`,
  `sqrt`, `tan`, `tanh`, `trunc`

---

## Supported syntax (group extensions)

These are **custom extensions** (not standard PromQL) parsed by `GroupPromQLParser`.

### `group_by[...]`

```
group_by_expr :=
  "group_by" "[" column_list "]" "(" metric_selector ")"

column_list := IDENT { "," IDENT }
```

Example:
- `group_by[platform](sales{platform!="tablet"})`

### `*_by[...]` aggregations

Supported `*_by` functions:
- `sum_by`, `count_by`, `min_by`, `max_by`, `avg_by`, `stddev_samp_by`

Syntax:

```
agg_by_expr :=
  agg_by_name "[" column_list "]" "(" metric_selector "[" DURATION "]" ")"
```

Example:
- `sum_by[platform](sales{platform!="tablet"}[1y])`

### `_over_time` on group expressions

The group parser also supports applying `_over_time` to a **group expression**:

```
group_over_time :=
  agg_over_time_name "(" group_instant "[" DURATION "]" ")"
```

Example:
- `avg_over_time(sum_by[platform](sales[1h])[1y])` *(structure supported; practical usefulness depends on compiler behavior)*

---

## Not supported (common PromQL features)

This implementation does **not** currently support:
- Comparison operators: `> < >= <= ==`
- Boolean logic: `and`, `or`, `unless`
- Vector matching modifiers: `on(...)`, `ignoring(...)`, `group_left`, `group_right`
- Offset: `offset 5m`
- Regex label matchers: `=~`, `!~`
- Full subqueries: `(expr)[1h:5m]`
- Functions beyond the lists above (unless you extend the grammar + compiler)

---

## End-to-end example (works)

```
sum_by[platform](sales{platform!="tablet"}[1y])
```

Produces SQL similar to:
- `... FROM sales WHERE platform!="tablet" ...`


