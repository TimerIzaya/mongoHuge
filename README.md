### 对接前端可能遇到的问题

- **是否会删除name，如果是，删除模块需要额外考虑**
- **查询模块和增删改模块的路径拆分算法是根据 "." 拆分，所以KV查询里的K和V字符串暂时不能有 "."。**
- **目前暂不支持切片语法**
- **未做异常处理，目前只能处理合法的JsonPath**







### **增删改接口样例：**

- 由于引入拆分逻辑，查询和增删改模块都需要确认到最终要操作的文档。JsonPath分为外部路径和内部路径，外部路径为确认最终文档的路径，内部路径为针对当前文档的路径。比如"app.views[0].children[0].elements[0].name"，"views[0].children[0]"为外部路径，"elements[0].name"为内部路径。
- http请求携带appId，所以 "app" 不算外部路径
- 查询模块和增删改模块对外部路径定义不同，所以外部路径自行截取。通用方法为传入外部路径JsonPath，返回List 对象。



#### **Create**

- **存在外部路径**

- - **存在内部路径**

- - "path": "app.**views[2].children[3].children[4]**.elements[5].properties[6]"

- **无内部路径**（需要同步更新app结构）

- - "path": "app.**views[2].children[3].children[4]**"

- **无外部路径**

- - "path": "app.processes[2].properties[6]"



**Update**

- **存在外部路径**

- - **存在内部路径**

- - "path": "app.**views[2].children[3].children[4]**.elements[5].properties[6]"

- **无内部路径**

- - **更新普通字段**

- - "path": "app.**views[2].children[3].children[4]**"  -> {type: "xxx"}

- **更新name字段**（需要同步更新app结构中对应的name，因为name保留用于查询）

- - "path": "app.**views[2].children[3].children[4]**"  -> {name: "xxx"}

- **更新children数组**（需要同步更新app结构）

- - "path": "app**.views[2].children[3].children[4]**"    -> {children: {[],[],[]}}

- **无外部路径**

- - **更新普通字段**

- - "path": "app.processes[2].properties[6]" -> {type:"xxx"}

- **更新views数组**（需要同步更新app结构）

- - - - "path": "app" -> {views:{[],[],[]}}

- **更新logic数组****（需要同步更新app结构）
- - - "path": "app" -> {logics:{[],[],[]}}

**Delete**

- **存在外部路径**

- - **存在内部路径**

- - "path": "app.**views[2].children[3].children[4]**.elements[5].properties[6]"
- "path": "app.**views[2].children[3].children[4]**.type"
- "path": "app.**views[2].children[3].children[4]**.name"（需要确认需求，是否可能删除name，如果是，也要更新app结构）
- "path": "app.**views[2]**.children" （需要同步更新app结构）

- **无内部路径**

- - **删除view或者logic**

- - "path": "app.**views[2].children[3].children[4]**"  （需要同步更新app结构）
- "path": "app.logics[2]" （需要同步更新app结构）

- **无外部路径**

- - "path": "app.processes[0]"
- "path": "app.logics"（需要同步更新app结构）
- "path": "app.views"（需要同步更新app结构



**创建整个应用**

"path": "app"

**删除整个应用**

"path": "app"  