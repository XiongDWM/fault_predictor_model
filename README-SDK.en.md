## FaultPredictor SDK — Training Data Generation Toolkit
[English](README-SDK.en.md) | [中文](README-SDK.md)

This SDK lives in the package `com.xiongdwm.faultpredictor.sdk` and provides lightweight utilities for generating training data on the business side. Its goal is to convert raw business data (for example GPS coordinates, categorical strings, etc.) into stable numeric features suitable for training and prediction pipelines, including stream learners such as Hoeffding Trees.

### Key Features
- GPS → grid ID (`GpsGridMapping`): map latitude/longitude to a fixed grid index, with configurable grid size and bounding box.
- Stable string mapping (`StringUtils`): 32-bit and 64-bit stable hash-to-double mappings that allow you to map categorical strings into numbers without maintaining a full vocabulary.
- Examples and utilities: guidance in the README covers practical usage (how to discretize a hash into buckets, how to store categories as integer columns in a record, etc.).

### Use Cases
- Bulk or streaming generation of training data in business pipelines (feature engineering stage).
- Cases where you only need "same text → same numeric value" and do not rely on semantic similarity.
- When downstream systems accept only numeric inputs, you can compress categorical features into integer buckets for compatibility.

---

## Quick Start

Build (the project uses Gradle):
```bash
./gradlew build
```

If you added the optional `sdkJar` task to `build.gradle`, you can create an SDK-only jar with:
```bash
./gradlew sdkJar
```

---

## API Overview (matching current source)

- `com.xiongdwm.faultpredictor.sdk.StringUtils`
  - `public static double oneHotEncode(String value)`  
    Uses Java's `String.hashCode()` to map a string to `[0,1)` (32-bit mapping). Current implementation returns `0.0` for null/empty strings.
  - `public static double oneHotEncode64(String value)`  
    Uses a 64-bit non-cryptographic stable hash (FNV-like), takes the low 53 bits and maps to `[0,1)` to reduce collisions compared to 32-bit hashing.
  - (internal) `private static long simpleStableHash64(String s)`: 64-bit hash implementation.

- `com.xiongdwm.faultpredictor.sdk.GpsGridMapping`
  - `public GpsGridMapping()`
  - `public GpsGridMapping(double gridSize, double minLon, double maxLon, double minLat, double maxLat)`
  - `public double toGridIndex(double latitude, double longitude)`
    Maps latitude/longitude to a gridIndex (returned as a double representing an integer index). The default implementation covers the Chengdu area with a default `gridSize=0.005` (approx. 500m).
  - Note: `gridIndexToCenter` is currently private in the source. If you need to retrieve the grid center from an index, consider making that method public.

---

## Examples

- String mapping (recommended: discretize / index instead of using raw fractional hash directly)
```java
import com.xiongdwm.faultpredictor.sdk.StringUtils;

public class Demo {
    public static void main(String[] args) {
        String a = "主网一级";
        String b = "主网四级";

        double d1 = StringUtils.oneHotEncode64(a); // 64-bit mapping -> [0,1)
        double d2 = StringUtils.oneHotEncode64(b);

        // If your model or pipeline expects a single numeric column, discretize into buckets
        int buckets = 100;
        int bucketA = (int) (d1 * buckets); // 0 .. buckets-1
        int bucketB = (int) (d2 * buckets);

        // Write bucket into a fixed record column (e.g. record[offset] = bucketA)
        System.out.println("bucketA=" + bucketA + ", bucketB=" + bucketB);
    }
}
```

- GPS grid mapping
```java
import com.xiongdwm.faultpredictor.sdk.GpsGridMapping;

public class GpsDemo {
    public static void main(String[] args) {
        GpsGridMapping mapping = new GpsGridMapping(); // default grid size and bounds
        double gridIndex = mapping.toGridIndex(30.663648, 104.073556);
        System.out.println("gridIndex=" + gridIndex);
        // If you need the cell center, gridIndexToCenter exists but is currently private.
        // Consider making it public if you need it.
    }
}
```

---

## Practical Guidance (important)
- Do not treat the fractional values returned by `oneHotEncode`/`oneHotEncode64` as semantically ordered or continuous features. If your goal is "same category -> same value" for tree-based downstream models, we recommend:
  - discretizing the hash into `bucket = floor(d * buckets)` and storing the bucket integer in a single column; or
  - if you can maintain a vocabulary, using ordinal indices (0..K-1) or one-hot encoding (if category count is small).
- Bucket sizing (rule of thumb):
  - small vocabulary and maintainable: use vocabulary + ordinal / one-hot;
  - medium cardinality (tens to hundreds): buckets = 100~1000;
  - high cardinality (thousands+): use larger bucket counts or multiple hash features.
- For streaming learners such as Hoeffding Trees, discrete integer buckets are generally easier for split criteria to use than uniformly distributed fractional hashes.
- Keep mapping stable (do not change hash function or bucket count in production) to avoid inconsistency between historical and new data.

---

## Suggested small improvements (for developers)
- Make `GpsGridMapping.gridIndexToCenter` public so callers can recover cell centers from grid indices.
- Add helper APIs to `StringUtils` such as:
  - `bucketByHash64(String value, int buckets)` to return 0..buckets-1 directly; and
  - `ordinalEncode` / `fillOneHotInto` if you plan to support vocab-based encoding.
- If you publish a standalone SDK jar, consider adding an `sdkJar` Gradle task that packages the `com/xiongdwm/faultpredictor/sdk/**` classes and any required POJOs (for example `TrainingDataPackage`).

---

## FAQ
Q: Can I feed `oneHotEncode64` directly into a tree model?
A: While possible, it is not recommended to treat the fractional result as an ordered continuous feature. Discretization or ordinal indexing is more reliable for tree-based learners.

Q: Can the grid bounds be customized?
A: Yes — use the constructor `new GpsGridMapping(gridSize, minLon, maxLon, minLat, maxLat)`.

---

## License & Contribution
This SDK follows the LICENSE at the project root (if present). Contributions via issues or PRs are welcome.
