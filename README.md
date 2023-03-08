# html2pdf

项目下载到本地后运行
访问 http://127.0.0.1:8080/contract/previewCodePdf?firstParty=甲方&secondParty=乙方 预览pdf

访问 http://127.0.0.1:8080/contract/downloading?firstParty=甲方&secondParty=乙方 下载pdf

文档中甲方乙方两个变量通过 firstParty 与 secondParty 控制

由于示例中没有使用数据库，直接使用的文件保存，pdf源文件在 document/LaborContract.html 中
动态存储的html转为pdf导出 盖章等操作

