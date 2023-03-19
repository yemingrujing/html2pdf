package html2pdf.html2pdf.itext;

import cn.hutool.core.util.StrUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.Pipeline;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import html2pdf.html2pdf.itext.fontProvider.MyFontsProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static freemarker.template.Configuration.AUTO_DETECT_TAG_SYNTAX;

public class YtPDFComponent {

    private YtPDFWorkStreamInterface ytPDFWorkStreamInterface;

    public YtPDFComponent(YtPDFWorkStreamInterface ytPDFWorkStreamInterface) {
        this.ytPDFWorkStreamInterface = ytPDFWorkStreamInterface;
    }


    /**
     * 获取完整网页信息
     *
     * @param templateString 网页模板信息
     * @param data           数据
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    private String ytGetHtmlString(String templateString, Map data) throws IOException, TemplateException {
        Configuration cfg = new Configuration();
        cfg.setTagSyntax(AUTO_DETECT_TAG_SYNTAX);
        //无视掉空制的异常
        cfg.setClassicCompatible(true);
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("myTemplate", templateString);
        cfg.setTemplateLoader(stringLoader);
        StringWriter writer = new StringWriter();
        Template template = cfg.getTemplate("myTemplate", "utf-8");
        template.process(data, writer);

        // 格式化html
        org.jsoup.nodes.Document doc = Jsoup.parse(writer.toString());
        // 去除过大的宽度
        String style = doc.attr("style");
        if ((!style.isEmpty()) && style.contains("width")) {
            doc.attr("style", "");
        }
        Elements divs = doc.select("div");
        for (Element div : divs) {
            String divStyle = div.attr("style");
            if ((!divStyle.isEmpty()) && divStyle.contains("width")) {
                div.attr("style", "");
            }
        }
        // jsoup生成闭合标签
        doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        return doc.toString();

    }

    /**
     * 获取完整的pdf数据流
     *
     * @param html
     * @return
     * @throws DocumentException
     * @throws IOException
     */
    private byte[] ytGetPDFStream(String html) throws DocumentException, IOException {
        // step 1
        Document document = new Document();
        ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, tempOutputStream);
        document.open();
        InputStream in_withcode = new ByteArrayInputStream(html.getBytes("UTF-8"));
        //载入自定义解析器来解决中文不显示问题
        MyFontsProvider fontProvider = new MyFontsProvider();
        fontProvider.addFontSubstitute("lowagie", "garamond");
        fontProvider.setUseUnicode(true);
        //使用我们的字体提供器，并将其设置为unicode字体样式
        CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);
        HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
        htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
        CSSResolver cssResolver = XMLWorkerHelper.getInstance().getDefaultCssResolver(true);
        Pipeline<?> pipeline = new CssResolverPipeline(cssResolver, new HtmlPipeline(htmlContext, new PdfWriterPipeline(document, writer)));
        XMLWorker worker = new XMLWorker(pipeline, true);
        XMLParser p = new XMLParser(worker);
        p.parse(new InputStreamReader(in_withcode, "UTF-8"));
        document.close();
        return tempOutputStream.toByteArray();

    }

    public void ytStringToPDFStream(String templateString, OutputStream outputStream, Map data) {
        byte[] newPdfBate = null;
        try {
            String html = this.ytGetHtmlString(templateString, data);
            byte[] pdfBate = this.ytGetPDFStream(html);
            newPdfBate = this.ytPDFWorkStreamInterface.onTransformSuccess(pdfBate);
            outputStream.write(newPdfBate);
        } catch (Exception e) {
            e.printStackTrace();
            this.ytPDFWorkStreamInterface.onTransformError(e);
        } finally {
            try {
                this.ytPDFWorkStreamInterface.onTransformComplete(newPdfBate);
            } catch (Exception e) {
                e.printStackTrace();
                this.ytPDFWorkStreamInterface.onTransformError(e);
            }
        }
    }

    /**
     * 替换字符串
     *
     * @param docxContent
     * @param replaceMap
     * @param regex
     * @return
     */
    public static String replaceStrElementValue(String docxContent, Map<Integer, String> replaceMap, String regex) {
        if (StrUtil.isNotBlank(docxContent) && Objects.nonNull(replaceMap) && !replaceMap.isEmpty()) {
            Matcher matcher = Pattern.compile(regex).matcher(docxContent);
            if (matcher.find(0)) {
                matcher.reset();
                StringBuffer sb = new StringBuffer();
                Integer matcherStart = 1;
                while (matcher.find()) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(StrUtil.blankToDefault(replaceMap.get(matcherStart), "")));
                    matcherStart++;
                }
                matcher.appendTail(sb);
                return sb.toString();
            }
        }
        return docxContent;
    }
}
