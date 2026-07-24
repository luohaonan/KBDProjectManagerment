# 强制代码查询规则（本Java后端专用）
## 1 核心红线：禁止批量读取源码
任何需要查询代码结构的场景，**严禁一次性读取多个.java文件、全盘遍历src目录**，避免大量消耗Token。
只允许修改代码时读取1~2个目标文件。

## 2 结构化信息必须走Graphify终端查询
需要获取以下信息，必须调用Terminal MCP执行graphify命令，不许读源码：
1. Spring Controller 接口、URL、请求方式
2. Java类的字段、方法、注解、父类、实现接口
3. 方法调用链路、哪些地方引用了指定函数
4. @Mapper、@Service、@RestController等注解筛选
5. 实体类、数据库映射关系

## 3 Graphify固定调用命令（必须携带目录参数）
项目索引目录：`./graphify-out`，所有命令强制加 `--output-dir graphify-out`
1. 查全部后端接口
python -m graphify query controller --output-dir graphify-out
2. 根据类名查完整结构
python -m graphify query class --name 填写类名 --output-dir graphify-out
3. 查询方法调用关系
python -m graphify query call --method 填写方法名 --output-dir graphify-out
4. 根据注解筛选所有类
python -m graphify query annotation --anno 注解全路径 --output-dir graphify-out

## 4 索引更新要求
完成代码修改、新增接口/实体后，主动执行增量刷新：
python -m graphify rebuild --output-dir graphify-out
大范围重构执行全量重建：
python -m graphify build --output-dir graphify-out

## 5 执行逻辑顺序
查结构 → 调用Terminal跑graphify；
改代码 → 读取单个文件；
绝不颠倒。