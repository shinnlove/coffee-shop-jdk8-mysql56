# 原始需求与参考文件

以下四份原件保留原始字节，便于 Git 追踪和对照；不要用修改过的实现契约覆盖原件。

| 原件 | 用途 |
| --- | --- |
| [01_Shared_Coffee_Shop_Specification_v1_1.docx](01_Shared_Coffee_Shop_Specification_v1_1.docx) | 通用业务与质量要求 |
| [04_Backend_Engineer_Senior_Assignment.docx](04_Backend_Engineer_Senior_Assignment.docx) | 高级后端作业、接口、测试和评审要求 |
| [openapi-draft.yaml](openapi-draft.yaml) | 原始接口契约 |
| [reference_fixtures.json](reference_fixtures.json) | 菜单、请求、响应和错误示例 |

实际实现的契约在 [根目录 openapi.yaml](../openapi.yaml)。示例是参考数据，不等于完整 HTTP 测试；本项目的脚本和自动化用例负责发送请求、串联状态并断言结果。
