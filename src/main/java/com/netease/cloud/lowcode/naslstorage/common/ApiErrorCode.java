package com.netease.cloud.lowcode.naslstorage.common;

/**
 * API 错误码
 *
 */
public enum ApiErrorCode {

    SUCCESS(200, "Success", "The Api was called successfully.", "调用成功"),
    //后续建议所有的数据库相关的错误都返回此code 不需要指定errorMsg 目前兼容了部分以前的提示信息
    INTERNAL_SERVER_ERROR(500, "InternalServerError", "internal server error", "内部服务出错"),
    //权限相关401保留
    PERMISSION_DENIAL(401, "PermissionDenial ", "user not login", "用户未登陆"),
    PERMISSION_DENIED(401, "Unauthorized", "permission denied", "无权限"),
    //第三方服务异常
    REMOTE_OBJECT_HAS_EXISTED(401001,"RemoteServerObjectHasExisted","Remote server object has existed","远程服务对象已存在"),
    REMOTE_OBJECT_HAS_NOT_EXISTED(401002,"RemoteServerObjectHasNotExisted","Remote server object has not existed","远程服务对象不存在"),
    REMOTE_REQUEST_ERROR(401003, "RemoteRequestError", "call remote server error: %s", "调用远程服务异常：%s"),
    NOS_REQUEST_ERROR(401004,"NosRequestError","Nos request error","第三方服务出错 %s"),
    RDS_REQUEST_ERROR(401005,"RdsRequestError","Rds request error","第三方服务出错 %s"),
    NCS_REQUEST_ERROR(401006,"NcsRequestError","Ncs request error","第三方服务出错 %s"),
    GW_REQUEST_ERROR(4011007,"GwRequestERROR","Gw request error","第三方服务出错 %s"),
    CICD_REQUEST_ERROR(401008,"CicdRequestError","Cicd request error","第三方服务出错 %s"),
    NUIMS_REQUEST_ERROR(401009,"NuimsRequestError","Nuims request error","第三方服务出错 %s"),
    GENERATOR_REQUEST_ERROR(401010,"GeneratorRequestError","Generator request error","第三方服务出错 %s"),
    HARBOR_REQUEST_ERROR(401011,"HarborRequestError","Harbor request error","第三方服务出错 %s"),
    GRAPH_REQUEST_ERROR(401012,"GraphRequestError","Graph request error","第三方服务出错 %s"),
    MESSAGE_REQUEST_ERROR(401013,"MessageRequestError","Message request error","第三方服务出错 %s"),
    GIT_REQUEST_ERROR(401014,"GitRequestError","Git request error","第三方服务出错 %s"),
    FILE_SERVER_REQUEST_ERROR(401015,"FileServerRequestError","File server request error","第三方服务出错 %s"),
    K8S_REQUEST_ERROR(401016,"K8sRequestError","K8s  request error","第三方服务出错 %s"),
    OWL_REQUEST_ERROR(401017,"OwlRequestError","Owl request error","第三方服务出错 %s"),

    //操作限制
    ASSET_NOT_ALLOWED_OPERATE(401101,"AssetNotAllowedOperate","Asset is not allowed operate,please check the owner of asset","资产不允许操作，请检查资产所属租户"),
    APP_STATUS_INVALID(401102,"AppStatusInvalid","App status invalid","应用当前状态不可用或正在执行初始化、保存和还原操作"),
    LAST_BATCH_EXCEL_IMPORT_NOT_FINISH(401103,"ExcelImportNotFinish","Last batch of Excel  do not finish","上一批次Excel内容导入未结束"),
    LAST_BATCH_SQL_IMPORT_NOT_FINISH(401104,"SqlImportNotFinish","Last batch of SQL  do not finish","上一批次SQL内容导入未结束"),
    APP_NOT_DEPLOY(401105,"AppNotDeploy","App do not deploy","应用未发布,请发布后再进行操作"),
    APP_DEPLOY_ING(401106,"AppDeployIng","App is deploying","应用正在发布,请不要重复发布"),
    IDE_NOT_ALLOWED_UPGRADE(401107,"IdeNotAllowedUpgrade","IdeVersion is not allowed to upgrade","该升级需要手动运维操作，请联系低代码平台开发人员"),
    DATABASE_UPDATE_NOT_ALLOW(401108, "DatabaseUpdateNotUpdate", "the database not allow update", "该数据库配置不允许修改"),
    NOS_TYPE_NOT_SUPPORT(401109,"NosTypeNotSupport","Nos type not support","对象存储类型%s不支持或配置缺失"),
    DB_TYPE_NOT_SUPPORT(401110,"DbTypeNotSupport","Db type not support","数据库类型%s不支持"),
    ASSET_NOT_SUPPORT_MODULE(401111,"ModuleNotSupport","Asset not support module","引用的资产类型或子类型不支持模块化"),
    FILE_SIZE_EXCEED_LIMIT(401112,"FileSizeExceedLimit","The file %s size exceeds the maximum limit","文件 %s 大小超出最大限制"),
    DB_DATA_TYPE_NOT_SUPPORT(401113,"DbDataTypeNotSupport","Db data type %s not support","数据库字段类型 %s 不支持"),
    TEMPLATE_TYPE_NOT_SUPPORT(401114,"TemplateTypeNotSupport","Template type not support","模板类型%s不支持"),
    //格式错误
    URL_FORMAT_ERROR(401201, "URLFormatError", "Url format error", "url 格式错误"),
    SQL_SCRIPT_FORMAT_ERROR(401202, "FormatError", "Sql script %s format error", "sql文件 %s 格式错误"),
    CRON_SYNTAX_ERROR(401203,"CronSyntaxError","Cron expression syntax error","表达式语法不正确，请根据规则说明进行检查"),
    JSON_SEQ_ERROR(401204,"JsonSyntaxError","Json syntax error","Json序列化失败"),
    TIME_FORMAT_MISMATCH(401205,"TimeFormatMismatch","Time format mismatch","时间格式%s不匹配"),
    //参数缺失或非法
    SIGNATURE_INVALID(401251,"SignatureInvalid","Signature invalid","签名失效"),
    ORDER_CLAUSE_INVALID(401252,"OrderClauseInvalid","Order clause invalid","排序参数非法"),
    PARAM_VALUE_INVALID(401253, "ParamValueInvalid", "the param %s value can't be %s", "参数 %s 的值不能为 %s"),
    PARAM_MISSING(401254, "ParamMissing", "The param %s is missing", "缺少必要的参数 %s"),
    //试用用户相关
    APPLY_COMPANY_NOT_EXISTED(401281,"ApplyCompanyNotExisted","The ApplyCompany %s has not existed","试用企业 %s 不存在"),
    APPLY_COMPANY_EXISTED(401282,"ApplyCompanyExisted","The ApplyCompany %s has existed","试用企业 %s 已存在"),
    APPLY_USER_ENV_INITIALIZING(401283,"ApplyUserEnvInitializing","Apply user %s environment  initializing","用户 %s 试用环境正在初始化"),
    APPLY_STATUS_INVALID(401284,"ApplyStatusInvalid","Apply status invalid","当前用户未试用或试用状态已到期"),
    APPLY_STATUS_VALID(401285,"ApplyStatusValid","Apply status valid","当前用户未试用或试用状态未到期"),
    APPLY_USER_NOT_EXISTED(401286,"ApplyUserNotExisted","The ApplyUser has not existed","企业试用用户 %s 不存在"),
    //租户相关
    TENANT_QUOTA_NOT_EXISTED(401331,"TenantQuotaNotExisted","Tenant quota not existed","租户配额信息不存在"),
    TENANT_EXISTED(401332,"TenantExisted","The Tenant %s has existed","租户 %s 已存在"),
    TENANT_NOT_EXISTED(401333,"TenantNotExisted","Tenant info has not existed","租户%s不存在或处于禁用状态"),
    TENANT_NOT_FORBIDDEN(401334,"TenantNotForbidden","Tenant is not forbidden","租户%s未被禁用"),
    INSUFFICIENT_RESOURCES(401335, "InsufficientResources", "Insufficient resources to create service.", "资源不足，无法创建服务"),
    USER_GROUP_NOT_EXISTED(401336,"UserGroupNotExisted","User group not existed","用户组不存在"),
    //应用信息相关
    LCPAPP_EXISTED(401381,"LcpAppExisted","The lcp app %s has existed","平台应用 %s 已存在"),
    LCPAPP_NOT_EXISTED(401382,"LcpAppNotExisted","The lcp app has not existed already","当前平台应用不存在"),
    MICRO_INFO_EXISTED(401383,"MicroInfoExisted","The microservice info has existed","微服务信息已存在"),
    MICRO_ENV_EXISTED(401384,"MicroEnvInfoExisted","The microservice env info has existed","微服务环境信息 %s已存在"),
    WEB_INFO_NOT_EXISTED(401385,"WebInfoNotExisted","The web info has not existed already","web服务不存在"),
    MICRO_INFO_NOT_EXISTED(401386,"MicroInfoNotExisted","The microservice info has not existed already","微服务信息不存在"),
    GIT_REPOSITORY_EXISTED(401387,"GitRepositoryExisted","The git repository has existed","git 仓库已存在"),
    GIT_PROJECT_EXISTED(401388,"GitProjectExisted","The git project has existed","git仓库 project：%s 已存在"),
    GIT_GROUP_EXISTED(401389,"GitGroupExisted","The git group has existed","git仓库 group：%s 已存在"),
    FRONTEND_FILE_EXISTED(401340,"FrontendFileExisted","The frontendFile have existed","前端文件已存在"),
    DB_INFO_NOT_EXISTED(401341,"DbInfoNotExisted","The db info has not existed already","数据库信息不存在"),
    IMPORT_SQL_BATCH_NOT_EXISTED(401342,"ImportSqlBatchNotExisted","The import sql batch not existed","导入的sql批次不存在"),
    ROLE_HAS_EXISTED(401343,"RoleDuplicated","The role %s has existed","角色 %s 已存在"),
    CICD_RECORD_NOT_EXISTED(401344,"CicdRecordNotExisted","Cicd record not existed","cicd记录不存在"),
    //资产相关
    MODULE_EXISTED(401431, "ModuleExisted", "lcp app module has existed", "模块 %s 已存在"),
    MODULE_NOT_EXISTED(401432,"ModuleInfoNotExisted","The module has not existed","模块 %s 不存在"),
    ASSET_VERSION_DSL_HAS_EXISTED(401433,"AssetVersionDslHasExisted","The AssertVersionDsl has existed","当前资产版本描述信息已存在"),
    ASSET_NOT_EXISTED(401434,"AssetHasNotExisted","The asset has not existed already","该资产%s不存在"),
    ASSET_EXISTED(401435,"AssetExisted","The asset has existed already","资产已存在"),
    ASSET_VERSION_NOT_EXISTED(401436,"AssetVersionNotExisted","The assetVersion has not existed already","该版本资产%s不存在"),
    ASSET_VERSION_DSL_NOT_EXISTED(401437,"AssetVersionDslNotExisted","The AssertVersionDsl has not existed already","版本资产%s内容描述不存在"),
    DEP_ASSET_VERSION_NOT_EXISTED(401438,"DepAssetVersionNotExisted","The dependent version asset does not exist already","所依赖版本资产%s不存在"),
    PAGE_TEMPLATE_NOT_EXISTED(401439,"PageTemplateNotExisted","PageTemplate has not existed","页面模板%s不存在"),
    APP_TEMPLATE_NOT_EXISTED(401440,"AppTemplateNotExisted","AppTemplate has not existed","应用模板%s不存在"),

    //平台版本配置依赖相关
    IDE_DEPENDENCY_EXISTED(401481,"IdeDependencyExisted","The ide dependencies has existed","开发环境依赖已存在"),
    IDE_VERSION_EXISTED(401482,"IdeVersionExisted","The ide version has existed","开发环境版本 %s 已存在"),
    IDE_VERSION_NOT_EXISTED(401483,"IdeVersionNotExisted","The ide version has not existed","开发环境版本不存在"),
    IDE_VERSION_LE_CURRENT(401484,"IdeVersionLowerOrEqualCurrent","The ide version is lower or equal to current","开发环境版本不能低于或等于当前版本"),
    IDE_DEPENDENCY_NOT_EXISTED(401485,"IdeDependencyNotExisted","The ide dependencies has not existed","平台依赖不存在"),
    PLATFORM_VERSION_EXISTED(401486,"PlatformVersionExisted","The platform version has existed","平台版本 %s 已存在"),
    PLATFORM_VERSION_NOT_EXISTED(401487,"PlatformVersionNotExisted","The platform version has not existed","平台版本不存在"),
    USER_CONFIG_MISSED(401488, "UserConfigMissed", "user config missed, please contact your administrator", "系统配置缺失或不存在，请联系管理员"),

    //应用版本相关
    APP_VERSION_EXISTED(401531,"AppVersionExisted","App version has existed","应用版本已存在"),
    APP_VERSION_NOT_EXISTED(401532,"AppVersionNotExisted","App version not existed","应用版本不存在"),
    APP_VERSION_NUM_EXCEED_LIMIT(401533,"AppVersionNumExceedLimit","Copy of application number has exceeded quota.", "副本数量已超过限额，无法创建应用"),
    OFFICIAL_APP_CANNOT_CREATE_COPY(401534, "OfficialAppCannotCreateCopy", "Official apps are not allowed to create copies.", "官方应用不允许创建副本应用"),
    COPY_APP_CANNOT_CREATED_BELOW_VERSION(401535, "CopyAppCannotCreatedBelowVersion", "Copy apps cannot be created below version %s.", "低于版本%s无法创建副本应用"),
    MAIN_APP_STATUS_ERROR(401536, "MainAppStatusError", "The master app is unavailable and cannot create a replica.", "主应用为不可用状态，无法创建副本"),
    MAIN_APP_NOT_EXIST(401537, "MainAppNotExist", "Main application does not exist.", "主应用不存在"),

    //网关导入接口相关
    API_SERVICE_NOT_EXISTED(401581,"ApiServiceNotExisted","The api service has not existed","API入口服务不存在"),
    API_INFO_NOT_EXISTED(401582,"ApiInfoNotExisted","The api base info has not existed","API基本信息不存在"),
    API_MODEL_NOT_EXISTED(401583,"ApiModelNotExisted","The api model  has not existed","API模块不存在"),
    API_MODEL_EXISTED(401584,"ApiModelExisted","The api model  has  existed","API模块 %s 已存在"),
    API_MODEL_HAS_QUOTED(401585,"ApiModelHasQuoted","The api model  has be quoted","API模块已被引用"),

    //多人协作
    COOPERATION_BRANCH_ASL_ERROR(401631, "BranchAslGetError", "Branch Asl get Error", "获取ASL失败"),
    COOPERATION_MASTER_BRANCH_NOT_FOUND(401632, "MasterBranchNotFound", "master branch not found", "未找到主应用"),
    COOPERATION_PULL_ERROR_NO_UPDATE(401633, "PullErrorBecauseOfNoUpdate", "pull error because of no update", "当前代码仓库无更新内容，无需拉取"),
    COOPERATION_PUSH_ERROR_MASTER_HAS_UPDATE(401634, "PushErrorBecauseOfMasterHasUpdate", "push error because of master has update", "代码仓库存在更新内容，请先拉取后再进行推送！"),
    COOPERATION_PULL_ERROR_DIFF_IDE_VERSION(401635, "PullErrorBecauseOfDiffIdeVersion", "pull error because of ide version is different", "代码仓库是更高版本IDE，请先升级IDE 版本后再拉取"),
    COOPERATION_VERSION_UPDATE_ERROR_MASTER_HAS_UPDATE(401636, "VersionUpdateErrorBecauseOfMasterHasUpdate", "update ide version error because of master has update", "代码仓库中存在更新内容，请先到可视化开发界面进行拉取操作后再尝试！"),
    COOPERATION_PUSHING(401637, "Pushing", "someone is pushing app", "有副本应用正在进行推送，请稍后尝试。"),
    COOPERATION_PULLING(401638, "Pulling", "someone is pulling app", "有副本应用正在进行拉取，请稍后尝试。"),
    COOPERATION_PULLING_NEED_UPDATE(401639, "Pulling", "code is update, please confirm.", "代码仓库或本地有更新,请重新比对合并结果。"),
    COOPERATION_PUSH_ERROR_IDE_VERSION(401640, "PushErrorIDEVersionNeedUpgrade", "Master or a copy app has a larger version, please upgrade after operation", "主应用或者副本应用有更大的版本，请升级后操作"),

    //应用状态相关
    WEB_GENERATOR_CALLBACK_FAILED(401681, "WebGeneratorCallBackFail", "the web generator callback failed", "前端代码生成服务回调失败"),
    MS_GENERATOR_CALLBACK_FAILED(401682, "MsGeneratorCallBackFail", "the micro generator callback failed", "后端代码生成服务回调失败"),
    FS_STATUS_APPINITING(401683, "FsIniting", "the file server is app initint", "文件服务器正在初始化中"),



    // 下列异常码定义且未使用 后续考虑移除
    TEMPLATE_CACHE_DIR_GET_FAIL(400, "TemplateCacheDirGetFail", "the template cache dir get failed", "获取模板缓存文件夹失败"),
    CICD_RECORD_NOT_FOUND(404, "CicdRecordNotFound", " CI/CD info is not existed, appId: %s", "对应 App 中找不到 CI/CD 记录。 appId: %s"),
    DELETE_DEPLOYMENT_ERROR(500, "deleteDeploymentError", "delete deployment error, error message:%s", "删除应用部署失败, 错误信息: %s"),
    DELETE_SERVICE_ERROR(500, "deleteServiceError", "delete service error, error message:%s", "删除应用服务失败，错误信息：%s"),
    DELETE_INGRESS_ERROR(500, "deleteIngressError", "delete ingress error, error message:%s", "删除应用服务发现失败，错误信息：%s"),
    TEMPLATE_CACHE_INIT_FAILED(400, "TemplateCacheInitFailed", "template cache init failed", "初始化web应用模板缓存目录失败"),
    LOCAL_REPO_CREATED_FAILED(400, "LocalRepoCreatedFailed", "local repository created failed", "服务端本地代码仓库创建失败"),
    VSCODE_STATUS_RUNNING(200, "VsCodeRunning", "the vs code is running", "VsCode 启动成功"),
    VSCODE_STATUS_STARTING(400, "VsCodeStarting", "the vs code is startting", "VsCode正在启动中"),
    VSCODE_STATUS_NOT_FOUND(404, "VsCodePodNotFound", "the vs code pod can't be found", "VsCode监听pod未找到"),
    VSCODE_STATUS_START_FAILED(500, "VsCodeStartFailed", "the vs code start failed: %s", "VsCode启动失败:%s"),
    GIT_GROUP_HAS_EXISTS(400, "GitGroupHasExists", "the git group %s has exists", "git仓库 group：%s 已存在"),
    GIT_PROJECT_HAS_EXISTS(400, "GitProjectHasExists", "the git project %s has exists", "git仓库 project：%s 已存在"),
    REMOTE_GIT_REPO_ADDR_INVALID(400, "RemoteGitRepoAddrInvalid", "remote git repository address invalid", "远程仓库地址不合法"),
    OFFICIAL_ASSETS_MISSING(400, "OfficialAssetsMissing", "the official assets is missing", "官方资产缺失"),
    PARAM_NOT_SUPPORT_VALUE(400, "ParameterNotSupported", "the param %s not support value %s", "该参数 %s 不支持该值 %s"),
    ;

    private int statusCode;
    private String code;
    private String enMessage;
    private String znMessage;

    ApiErrorCode(int statusCode, String code, String enMessage, String znMessage) {
        this.statusCode = statusCode;
        this.code = code;
        this.enMessage = enMessage;
        this.znMessage = znMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getEnMessage() {
        return enMessage;
    }

    public String getZnMessage() {
        return znMessage;
    }

    public boolean isThisCode(ApiBaseResult apiBaseResult) {
        return this.code.equals(apiBaseResult.getCode());
    }

    @Override
    public String toString() {
        return "ApiErrorCode{" + "statusCode=" + statusCode + ", code='" + code + '\'' + ", enMessage='" + enMessage
                + '\'' + ", znMessage='" + znMessage + '\'' + '}';
    }

}
