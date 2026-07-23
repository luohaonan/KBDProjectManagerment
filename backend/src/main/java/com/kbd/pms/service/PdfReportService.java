package com.kbd.pms.service;

import com.kbd.pms.entity.*;
import com.kbd.pms.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfReportService {

    private final ProjectRepository projectRepository;
    private final ProjectLevelRepository projectLevelRepository;
    private final IamUserRepository iamUserRepository;

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN = 54f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;
    private static final float COL_W = (CONTENT_W - 20) / 2;

    private static final Color C_BG     = new Color(0xFA, 0xF8, 0xF5);
    private static final Color C_DARK   = new Color(0x1A, 0x2A, 0x3A);
    private static final Color C_BODY   = new Color(0x3A, 0x3F, 0x47);
    private static final Color C_MUTED  = new Color(0x8A, 0x8E, 0x94);
    private static final Color C_ACCENT = new Color(0x4A, 0x6F, 0xA5);
    private static final Color C_RULE   = new Color(0xD0, 0xCE, 0xC9);

    private PDFont fTitle;
    private PDFont fBody;
    private PDDocument doc;
    private PDPageContentStream cs;
    private float y;

    public PdfReportService(ProjectRepository pr, ProjectLevelRepository plr, IamUserRepository iur) {
        this.projectRepository = pr;
        this.projectLevelRepository = plr;
        this.iamUserRepository = iur;
    }

    private void loadFonts(PDDocument doc) throws IOException {
        for (String cp : new String[]{"fonts/simhei.ttf","fonts/simkai.ttf","fonts/simfang.ttf"}) {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(cp);
            if (is != null) { try { fBody = PDType0Font.load(doc, is); is.close(); fTitle = fBody; return; } catch (IOException e) { try { is.close(); } catch (Exception e2) {} } }
        }
        for (String dir : new String[]{"C:\\Windows\\Fonts","/usr/share/fonts/truetype","/usr/share/fonts/opentype","/System/Library/Fonts"}) {
            File d = new File(dir); if (!d.isDirectory()) continue;
            File[] files = d.listFiles((dd,n) -> n.toLowerCase().endsWith(".ttf")||n.toLowerCase().endsWith(".ttc"));
            if (files==null) continue;
            for (File f : files) { if (f.length()<3_000_000) continue; try { fBody=PDType0Font.load(doc,f); fTitle=fBody; return; } catch (IOException ignored) {} }
        }
        throw new IOException("No CJK font found.");
    }

    @Transactional(readOnly = true)
    public byte[] generateInitiationReportPdf(long projectId) throws IOException {
        ProjectEntity p = projectRepository.findById(projectId).orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        ProjectLevelEntity lv = projectLevelRepository.findById(p.getLevelId()).orElseThrow(() -> new IllegalArgumentException("项目分级缺失"));
        String pmName = p.getPmUserId()!=null ? iamUserRepository.findById(p.getPmUserId()).map(IamUserEntity::getDisplayName).orElse("-") : "-";
        String initTime = p.getReviewSubmittedAt()!=null ? p.getReviewSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : (p.getCreatedAt()!=null ? LocalDateTime.ofInstant(p.getCreatedAt(),ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");

        doc = new PDDocument();
        loadFonts(doc);

        // -- Cover --
        PDPage cov = new PDPage(PDRectangle.A4); doc.addPage(cov);
        PDPageContentStream covCs = new PDPageContentStream(doc, cov); fillBg(covCs);
        float ty = PAGE_H * 0.68f;
        center(covCs,"创新药研发立项报告",fTitle,36,ty,C_DARK);
        center(covCs,"Drug Discovery Project Initiation Report",fBody,12,ty-26,C_MUTED);
        float iy = ty-56;
        center(covCs,p.getProjectCode(),fBody,15,iy,C_BODY);
        center(covCs,lv.getLevelName()+"  ·  "+lv.getLevelCode(),fBody,10,iy-22,C_MUTED);
        float by = MARGIN+30;
        center(covCs,LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),fBody,8,by,C_MUTED);
        center(covCs,"CONFIDENTIAL",fBody,7,by-14,C_MUTED);
        covCs.close();

        newContentPage();

        // 一、基本信息
        section("一、基本信息","Basic Information");
        kv2("项目名称",p.getProjectName(),"项目编号",p.getProjectCode());
        kv2("项目分级",lv.getLevelName()+"  ("+lv.getLevelCode()+")","项目经理",pmName);
        kv2("靶点 / 通路",ne(p.getTargetPathway()),"拟定适应症",ne(p.getIndication()));
        kv("发起时间",initTime); y-=2;
        blk("项目简介",ne(p.getTppSummary())); blk("详细描述",ne(p.getDescription())); y-=4;

        // 二、科学依据
        section("二、科学依据","Scientific Rationale");
        kv("靶点 / 通路",ne(p.getTargetPathway())); blk("生物学机制",ne(p.getMechanism()));
        blk("未满足的临床需求",ne(p.getUnmetNeeds())); blk("立项科学依据",ne(p.getScientificBasis())); y-=4;

        // 三、目标产品概览
        section("三、目标产品概览 (TPP)","Target Product Profile");
        kv2("预期适应症",ne(p.getExpectedIndication()),"给药途径",ne(p.getAdministrationRoute()));
        kv2("剂型",ne(p.getDosageForm()),"给药频率",ne(p.getDosageFrequency()));
        blk("预期疗效指标",ne(p.getEfficacyTarget())); blk("安全性优势",ne(p.getSafetyAdvantage())); y-=4;

        // 四、核心差异化优势
        section("四、核心差异化优势","Competitive Differentiation");
        blk("与现有/在研竞品相比的核心优势",ne(p.getDifferentiation())); y-=4;

        // 五、预算与计划日期
        section("五、预算与计划日期","Budget & Timeline");
        kv2("项目总预算",fmtYuan(p.getBudgetTotal()),"阶段预算至 PCC",fmtYuan(p.getBudgetToPcc()));
        kv("预估 PCC 提名日期",fmt(p.getPlannedPccDate())); kv("预估 IND 获批日期",fmt(p.getPlannedIndDate()));
        kv("预估 NDA 获批日期",fmt(p.getPlannedNdaDate())); y-=4;

        // 六、项目风险评估
        section("六、项目风险评估","Risk Assessment");
        blk("科学风险",ne(p.getRiskScientific())); blk("竞争风险",ne(p.getRiskCompetitive()));
        blk("注册与法规路径风险",ne(p.getRiskRegulatory())); y-=4;

        // 七、资源建议与所需支持
        section("七、资源建议与所需支持","Resources & Support Required");
        blk("简述需要 PMC 提供的资源或决策支持",ne(p.getSuggestionAndSupport()));

        // 尾部：固定在当前页底部
        // 无论上方内容停在何处，在当前已开启的最后一页的底部固定绘制 END OF REPORT
        y = MARGIN + 30;
        centerCs("— END OF REPORT —",fBody,8,y,C_MUTED);

        cs.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); doc.save(baos); doc.close();
        return baos.toByteArray();
    }

    private void ensureSpace(float needed) throws IOException { if (y-needed<MARGIN) newContentPage(); }
    private void newContentPage() throws IOException {
        if (cs!=null) cs.close();
        PDPage pg = new PDPage(PDRectangle.A4); doc.addPage(pg);
        cs = new PDPageContentStream(doc,pg); fillBg(cs); y = PAGE_H-MARGIN;
    }
    private void fillBg(PDPageContentStream c) throws IOException { c.setNonStrokingColor(C_BG); c.addRect(0,0,PAGE_W,PAGE_H); c.fill(); }

    /** 节标题：若当前页剩余空间不足 200pt，直接新开一页顶格开始 */
    private void section(String cn,String en) throws IOException {
        if (y - 200 < MARGIN) newContentPage();
        ensureSpace(54);
        cs.setStrokingColor(C_ACCENT); cs.setLineWidth(2f);
        cs.moveTo(MARGIN,y-2); cs.lineTo(MARGIN,y-22); cs.stroke();
        draw(cs,cn,fTitle,17,C_DARK,MARGIN+10,y-14);
        draw(cs,en.toUpperCase(),fBody,8,C_MUTED,MARGIN+10,y-28);
        y-=36;
        cs.setStrokingColor(C_RULE); cs.setLineWidth(0.3f);
        cs.moveTo(MARGIN,y); cs.lineTo(MARGIN+CONTENT_W,y); cs.stroke();
        y-=14;
    }
    private void center(PDPageContentStream c,String s,PDFont f,float sz,float dy,Color clr) throws IOException {
        float tw = f.getStringWidth(s)/1000*sz; draw(c,s,f,sz,clr,(PAGE_W-tw)/2,dy);
    }
    private void centerCs(String s,PDFont f,float sz,float dy,Color clr) throws IOException {
        float tw = f.getStringWidth(s)/1000*sz; draw(cs,s,f,sz,clr,(PAGE_W-tw)/2,dy);
    }
    private void kv(String key,String val) throws IOException {
        ensureSpace(20);
        draw(cs,key+"：",fBody,11,C_MUTED,MARGIN,y);
        float kw = fBody.getStringWidth(key+"：")/1000*11f+6;
        draw(cs,val,fBody,11,C_BODY,MARGIN+kw,y); y-=20;
    }
    private void kv2(String k1,String v1,String k2,String v2) throws IOException {
        ensureSpace(20);
        float x2 = MARGIN+COL_W+20;
        draw(cs,k1+"：",fBody,11,C_MUTED,MARGIN,y);
        draw(cs,v1,fBody,11,C_BODY,MARGIN+fBody.getStringWidth(k1+"：")/1000*11f+4,y);
        draw(cs,k2+"：",fBody,11,C_MUTED,x2,y);
        draw(cs,v2,fBody,11,C_BODY,x2+fBody.getStringWidth(k2+"：")/1000*11f+4,y);
        y-=20;
    }
    private void blk(String label,String ct) throws IOException {
        ensureSpace(28); draw(cs,label+"：",fBody,11,C_MUTED,MARGIN,y); y-=18;
        String txt = (ct==null||ct.isBlank())?"暂无":ct; float fs=10f, lh=fs*1.75f;
        for (String ln : wrap(txt,fBody,fs,CONTENT_W-4)) { ensureSpace(lh); draw(cs,ln,fBody,fs,C_BODY,MARGIN+4,y); y-=lh; } y-=3;
    }
    private void draw(PDPageContentStream c,String s,PDFont f,float sz,Color clr,float x,float dy) throws IOException {
        c.setNonStrokingColor(clr); c.beginText(); c.setFont(f,sz); c.newLineAtOffset(x,dy);
        for (int i=0;i<s.length();i++) {
            char ch = s.charAt(i);
            String glyph = switch(ch){ case '\u00A5'->"\uFFE5"; case '\u00A3'->"\uFFE1"; case '\u2212'->"\uFF0D"; default->String.valueOf(ch); };
            try { c.showText(glyph); } catch (Exception ex) { try { c.showText("?"); } catch (Exception ex2) {} }
        }
        c.endText();
    }
    private List<String> wrap(String txt,PDFont f,float fs,float mw) throws IOException {
        List<String> lines = new ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (char ch : txt.toCharArray()) {
            if (f.getStringWidth(cur.toString()+ch)/1000*fs>mw && cur.length()>0) { lines.add(cur.toString()); cur=new StringBuilder(String.valueOf(ch)); }
            else cur.append(ch);
        }
        if (cur.length()>0) lines.add(cur.toString()); if (lines.isEmpty()) lines.add("暂无"); return lines;
    }

    private String ne(String s) { return (s==null||s.isBlank())?"-":s; }
    private String fmt(LocalDate d) { return d!=null?d.format(DateTimeFormatter.ISO_LOCAL_DATE):"未设定"; }
    private String fmtYuan(java.math.BigDecimal amt) {
        if (amt==null||amt.compareTo(java.math.BigDecimal.ZERO)<=0) return "未设定";
        return "\uFFE5"+String.format("%,.2f",amt)+" 元";
    }
}