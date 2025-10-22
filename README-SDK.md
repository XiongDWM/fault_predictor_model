## FaultPredictor SDK — 训练数据生成工具包
[English](README-SDK.en.md) | [中文](README-SDK.md)

该 SDK 为业务侧生成训练数据提供的轻量工具。目标是把业务原始数据稳定映射为训练/预测可用的数值特征，便于下游模型（包括流式学习器如 Hoeffding Tree）使用。

### 主要功能
- GPS → 网格 ID（`GpsGridMapping`）：把经纬度映射到固定网格索引，支持自定义网格尺寸与覆盖范围。
- 字符串稳定映射（`StringUtils`）：提供 32-bit/64-bit 的稳定哈希到 double 的映射方法，便于在不维护完整词表情况下把类别映射为数值。
- 示例/工具：README 包含常见用法建议（如何把哈希离散化为桶、如何把类别作为单列整数写入 record 等）。

### 适用场景
- 业务侧需要批量或流式生成训练数据（特征工程阶段）。
- 需要稳定映射“相同文本 → 相同数值”但不关心语义相似性的场景。
- 与只接受数值输入的模型（或现有 pipeline）兼容时，把类别压缩为整数桶写入固定列。

---


## API 快览（当前源码对应）

- `com.xiongdwm.faultpredictor.sdk.StringUtils`
  - `public static double oneHotEncode(String value)`  
    以 Java `String.hashCode()` 为基础，把字符串稳定映射到 `[0,1)`（32-bit 映射）。对 null/空字符串当前实现返回 `0.0`。
  - `public static double oneHotEncode64(String value)`  
    使用 64-bit 非加密稳定哈希（FNV-like），取低 53 位并映射到 `[0,1)`，用于更低碰撞率的映射。
  - （内部）`private static long simpleStableHash64(String s)`：64-bit 非加密哈希实现。

- `com.xiongdwm.faultpredictor.sdk.GpsGridMapping`
  - `public GpsGridMapping()`
  - `public GpsGridMapping(double gridSize, double minLon, double maxLon, double minLat, double maxLat)`
  - `public double toGridIndex(double latitude, double longitude)`
    把经纬度映射到一个 gridIndex（double 表示的整数索引）。当前实现默认覆盖成都附近区域，默认 gridSize=0.005（约 500m）。
  - 注意：源码中 `gridIndexToCenter` 为 private（若需要将 gridIndex 转回中心点，建议把该方法改为 public）。

---

## 使用示例

- 字符串映射（稳定桶/索引建议）
```java
import com.xiongdwm.faultpredictor.sdk.StringUtils;

public class Demo {
    public static void main(String[] args) {
        String a = "你好";
        String b = "你是谁";

        double d1 = StringUtils.oneHotEncode64(a); // 64-bit 映射，返回 [0,1)
        double d2 = StringUtils.oneHotEncode64(b);

        // 若模型或 pipeline 只接受单列数值，建议把映射离散化为 bucket（整数）
        int b1= bucketByHash64(a, 10);
        int b2= bucketByHash64(b, 10);


        System.out.println("bucketA=" + bucketA + ", bucketB=" + bucketB);
    }
}
```

- GPS 网格映射
```java

public class GpsDemo {
    public static void main(String[] args) {
        GpsGridMapping mapping = new GpsGridMapping(); // 使用默认 gridSize 与区域
        double gridIndex = mapping.toGridIndex(30.663648, 104.073556);
        System.out.println("gridIndex=" + gridIndex);
        // 若需要格子中心坐标，当前实现包含 gridIndexToCenter（private）；
    }
}
```

---

## 实践建议（重要）
- 不要直接把 `oneHotEncode`/`oneHotEncode64` 的连续小数当作“语义连续值”。如果你的目标是“相同类别写出相同值”且下游是决策树类型模型，推荐：
  - 把哈希映射离散化成 `bucket = floor(d * buckets)`（写入单列整数）；或
  - 若可维护词表，使用 `ordinal` 索引（0..K-1）或 one-hot（若类别数可控）。
- 离散桶的大小选择（经验值）：
  - 类别较少且可维护词表：使用词表 + ordinal / one-hot；
  - 类别中等（几十到几百）：buckets=100~1000；
  - 类别很多（成千/万）：buckets 更大或采用多哈希组合。
- 对于流式学习（如 Hoeffding Tree），离散整数桶比随机连续小数更易被分裂器利用。
- 保证 pipeline 中映射方法稳定不变（不要随意更换哈希函数或 buckets），否则历史数据与新数据不一致会破坏模型表现。


## 常见问题（FAQ）
Q: 是否可以直接把 `oneHotEncode64` 的返回值放入树模型？
A: 虽然可行，但不推荐直接把其连续小数当作有序、语义连续的数值。更可靠的方法是离散化为桶或使用 ordinal 索引 / one-hot。

Q: 网格是否可自定义？
A: 是的，使用带参数的构造器 `new GpsGridMapping(gridSize, minLon, maxLon, minLat, maxLat)`。

---

## 许可证 & 贡献
本 SDK 遵循项目根目录的 LICENSE（若有）。欢迎提交 issue / PR 来补充更多工具方法或范例。
