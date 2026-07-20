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

/**
 * PDF 报告生成服务 — Magazine Layout 风格
 *
 * <p>字体加载：classpath:fonts/ → 系统 CJK TTF 文件
 */
@Service
public class PdfReportService {

    private final ProjectRepository projectRepository;
    private final ProjectLevelRepository projectLevelRepository;
    private final IamUserRepository iamUserRepository;
    private final MilestoneDefRepository milestoneDefRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectBudgetSnapshotRepository projectBudgetSnapshotRepository;
    private final MilestoneHistoryRepository milestoneHistoryRepository;

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN = 54f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    private static final Color C_BG     = new Color(0xFA, 0xF8, 0xF5);
    private static final Color C_DARK   = new Color(0x1A, 0x2A, 0x3A);
    private static final Color C_BODY   = new Color(0x3A, 0x3F, 0x47);
    private static final Color C_MUTED  = new Color(0x8A, 0x8E, 0x94);
    private static final Color C_ACCENT = new Color(0x4A, 0x6F, 0xA5);
    private static final Color C_RULE   = new Color(0xD0, 0xCE, 0xC9);

    private PDFont fTitle;
    private PDFont fBody;

    public PdfReportService(
            ProjectRepository projectRepository,
            ProjectLevelRepository projectLevelRepository,
            IamUserRepository iamUserRepository,
            MilestoneDefRepository milestoneDefRepository,
            ProjectMilestoneRepository projectMilestoneRepository,
            ProjectBudgetSnapshotRepository projectBudgetSnapshotRepository,
            MilestoneHistoryRepository milestoneHistoryRepository) {
        this.projectRepository = projectRepository;
        this.projectLevelRepository = projectLevelRepository;
        this.iamUserRepository = iamUserRepository;
        this.milestoneDefRepository = milestoneDefRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.projectBudgetSnapshotRepository = projectBudgetSnapshotRepository;
        this.milestoneHistoryRepository = milestoneHistoryRepository;
    }

    private void loadFonts(PDDocument doc) throws IOException {
        // 1) classpath 内置
        String[] classpathFonts = { "fonts/simhei.ttf", "fonts/simkai.ttf", "fonts/simfang.ttf" };
        for (String cp : classpathFonts) {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(cp);
            if (is != null) {
                try {
                    fBody = PDType0Font.load(doc, is);
                    is.close();
                    fTitle = fBody;
                    return;
                } catch (IOException ignored) {
                    try { is.close(); } catch (Exception e) {}
                }
            }
        }

        // 2) 系统字体目录
        String[] dirs = {
            "C:\\Windows\\Fonts",
            "/usr/share/fonts/truetype",
            "/usr/share/fonts/opentype",
            "/System/Library/Fonts",
        };
        for (String dir : dirs) {
            File d = new File(dir);
            if (!d.isDirectory()) continue;
            File[] files = d.listFiles((d2, n) ->
                n.toLowerCase().endsWith(".ttf") || n.toLowerCase().endsWith(".ttc"));
            if (files == null) continue;
            for (File f : files) {
                if (f.length() < 3_000_000) continue; // skip non-CJK
                try {
                    fBody = PDType0Font.load(doc, f);
                    fTitle = fBody;
                    return;
                } catch (IOException ignored) { /* try next */ }
            }
        }

        throw new IOException(
            "No CJK font found. Put simhei.ttf in src/main/resources/fonts/");
    }

    // ---------------------------------------------------------------
    // 1. 立项报告 PDF
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] generateInitiationReportPdf(long projectId) throws IOException {
        ProjectEntity p = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: id=" + projectId));
        ProjectLevelEntity lv = projectLevelRepository.findById(p.getLevelId())
                .orElseThrow(() -> new IllegalArgumentException("项目分级缺失"));

        String initiator = uname(p.getInitiatorUserId(), p.getPmUserId());
        String initTime = p.getReviewSubmittedAt() != null
                ? p.getReviewSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : (p.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(p.getCreatedAt(), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "");

        PDDocument doc = new PDDocument();
        loadFonts(doc);

        // -- Cover --
        PDPage cov = new PDPage(PDRectangle.A4);
        doc.addPage(cov);
        PDPageContentStream cs = new PDPageContentStream(doc, cov);
        fill(cs);
        float ty = PAGE_H * 0.52f;
        center(cs, "小分子创新药研发立项报告", fTitle, 36, ty, C_DARK);
        center(cs, "Drug Discovery Project Initiation Report", fBody, 12, ty - 26, C_MUTED);
        float ly = ty - 48;
        centerRule(cs, ly, 0.5f, C_ACCENT, 80);
        centerRule(cs, ly + 4, 0.3f, C_RULE, 55);
        float iy = ly - 36;
        center(cs, p.getProjectCode(), fBody, 15, iy, C_BODY);
        center(cs, lv.getLevelName() + "  ·  " + lv.getLevelCode(), fBody, 10, iy - 20, C_MUTED);
        float by = MARGIN + 30;
        center(cs, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), fBody, 8, by, C_MUTED);
        center(cs, "CONFIDENTIAL", fBody, 7, by - 14, C_MUTED);
        cs.close();

        // -- Content --
        PDPage pg = new PDPage(PDRectangle.A4);
        doc.addPage(pg);
        cs = new PDPageContentStream(doc, pg);
        fill(cs);
        float y = PAGE_H - MARGIN;

        y = sec(cs, "一、基本信息", "Basic Information", y);
        y = kv(cs, "项目名称", p.getProjectName(), y);
        y -= 2;
        y = kv(cs, "项目编号", p.getProjectCode(), y);
        y = kv(cs, "项目分级", lv.getLevelName() + "  (" + lv.getLevelCode() + ")", y);
        y = kv(cs, "靶点 / 通路", ne(p.getTargetPathway()), y);
        y = kv(cs, "拟定适应症", ne(p.getIndication()), y);
        y = kv(cs, "发起人", initiator, y);
        y = kv(cs, "发起时间", initTime, y);
        y -= 6;
        y = blk(cs, "项目简介", ne(p.getTppSummary()), y);
        y -= 2;
        y = blk(cs, "详细描述", ne(p.getDescription()), y);
        y -= 8;

        y = sec(cs, "二、科学依据", "Scientific Rationale", y);
        y = kv(cs, "靶点 / 通路", ne(p.getTargetPathway()), y);
        y = blk(cs, "生物学机制", ne(p.getMechanism()), y);
        y = blk(cs, "未满足的临床需求", ne(p.getUnmetNeeds()), y);
        y = blk(cs, "立项科学依据", ne(p.getScientificBasis()), y);
        y -= 8;

        y = sec(cs, "三、目标产品概览  (TPP)", "Target Product Profile", y);
        y = kv(cs, "预期适应症", ne(p.getExpectedIndication()), y);
        y = kv(cs, "给药途径", ne(p.getAdministrationRoute()), y);
        y = kv(cs, "剂型", ne(p.getDosageForm()), y);
        y = kv(cs, "给药频率", ne(p.getDosageFrequency()), y);
        y = blk(cs, "预期疗效指标", ne(p.getEfficacyTarget()), y);
        y = blk(cs, "安全性优势", ne(p.getSafetyAdvantage()), y);
        y -= 8;

        y = sec(cs, "四、核心差异化优势", "Competitive Differentiation", y);
        y = blk(cs, "与现有/在研竞品相比的核心优势", ne(p.getDifferentiation()), y);
        y -= 8;

        y = sec(cs, "五、预算与计划日期", "Budget & Timeline", y);
        String bt = p.getBudgetTotal() != null
                ? "¥" + String.format("%,.2f",
                    p.getBudgetTotal().divide(java.math.BigDecimal.valueOf(10000))) + " 万元"
                : "未设定";
        String bpc = p.getBudgetToPcc() != null
                ? "¥" + String.format("%,.2f",
                    p.getBudgetToPcc().divide(java.math.BigDecimal.valueOf(10000))) + " 万元"
                : "未设定";
        y = kv(cs, "项目总预算", bt, y);
        y = kv(cs, "阶段预算至 PCC", bpc, y);
        y = kv(cs, "预估 PCC 提名日期", fmt(p.getPlannedPccDate()), y);
        y = kv(cs, "预估 IND 获批日期", fmt(p.getPlannedIndDate()), y);
        y = kv(cs, "预估 NDA 获批日期", fmt(p.getPlannedNdaDate()), y);
        y -= 8;

        y = sec(cs, "六、项目风险评估", "Risk Assessment", y);
        y = blk(cs, "科学风险", ne(p.getRiskScientific()), y);
        y = blk(cs, "竞争风险", ne(p.getRiskCompetitive()), y);
        y = blk(cs, "注册与法规路径风险", ne(p.getRiskRegulatory()), y);
        y -= 8;

        y = sec(cs, "七、资源建议与所需支持", "Resources & Support Required", y);
        y = blk(cs, "简述需要 PMC 提供的资源或决策支持", ne(p.getSuggestionAndSupport()), y);
        y -= 14;

        centerRule(cs, MARGIN + 20, 0.3f, C_RULE, CONTENT_W);
        center(cs, "— END OF REPORT —", fBody, 7, MARGIN + 10, C_MUTED);

        cs.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }

    // ---------------------------------------------------------------
    // 绘制方法
    // ---------------------------------------------------------------

    private void fill(PDPageContentStream c) throws IOException {
        c.setNonStrokingColor(C_BG);
        c.addRect(0, 0, PAGE_W, PAGE_H);
        c.fill();
    }

    private float sec(PDPageContentStream c, String cn, String en, float y)
            throws IOException {
        if (y < MARGIN + 90) y = PAGE_H - MARGIN;
        c.setStrokingColor(C_ACCENT);
        c.setLineWidth(2f);
        c.moveTo(MARGIN, y - 2);
        c.lineTo(MARGIN, y - 20);
        c.stroke();
        text(c, cn, fTitle, 16, C_DARK, MARGIN + 10, y - 14);
        text(c, en.toUpperCase(), fBody, 8, C_MUTED, MARGIN + 10, y - 26);
        float ry = y - 34;
        c.setStrokingColor(C_RULE);
        c.setLineWidth(0.3f);
        c.moveTo(MARGIN, ry);
        c.lineTo(MARGIN + CONTENT_W, ry);
        c.stroke();
        return ry - 12;
    }

    private void center(PDPageContentStream c, String s, PDFont f,
                         float sz, float y, Color clr) throws IOException {
        float tw = f.getStringWidth(s) / 1000 * sz;
        float x = (PAGE_W - tw) / 2;
        text(c, s, f, sz, clr, x, y);
    }

    private void centerRule(PDPageContentStream c, float y, float w,
                              Color clr, float len) throws IOException {
        float x = (PAGE_W - len) / 2;
        c.setStrokingColor(clr);
        c.setLineWidth(w);
        c.moveTo(x, y);
        c.lineTo(x + len, y);
        c.stroke();
    }

    private float kv(PDPageContentStream c, String key, String val, float y)
            throws IOException {
        text(c, key + "：", fBody, 9.5f, C_MUTED, MARGIN, y);
        float kw = fBody.getStringWidth(key + "：") / 1000 * 9.5f + 6;
        text(c, val, fBody, 9.5f, C_BODY, MARGIN + kw, y);
        return y - 18;
    }

    private float blk(PDPageContentStream c, String label, String ct, float y)
            throws IOException {
        text(c, label + "：", fBody, 9.5f, C_MUTED, MARGIN, y);
        y -= 15;
        String txt = (ct == null || ct.isBlank()) ? "暂无" : ct;
        float fs = 9f;
        List<String> lines = wrap(txt, fBody, fs, CONTENT_W - 4);
        for (String ln : lines) {
            if (y < MARGIN + 25) break;
            text(c, ln, fBody, fs, C_BODY, MARGIN + 4, y);
            y -= fs * 1.65f;
        }
        return y - 4;
    }

    private void text(PDPageContentStream c, String s, PDFont f,
                       float sz, Color clr, float x, float y)
            throws IOException {
        c.setNonStrokingColor(clr);
        c.beginText();
        c.setFont(f, sz);
        c.newLineAtOffset(x, y);
        // 逐字符渲染 + try-catch 保护（应对 GSUB 回退）
        for (int i = 0; i < s.length(); i++) {
            try {
                c.showText(s.substring(i, i + 1));
            } catch (IOException e) {
                try { c.showText("?"); } catch (IOException e2) {}
            }
        }
        c.endText();
    }

    private List<String> wrap(String txt, PDFont f, float fs, float mw)
            throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char ch : txt.toCharArray()) {
            String cand = cur.toString() + ch;
            float w = f.getStringWidth(cand) / 1000 * fs;
            if (w > mw && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(String.valueOf(ch));
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.isEmpty()) lines.add("暂无");
        return lines;
    }

    // ---- utils ----

    private String ne(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private String fmt(LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ISO_LOCAL_DATE) : "未设定";
    }

    private String uname(Long a, Long b) {
        Long uid = a != null ? a : b;
        if (uid != null) {
            return iamUserRepository.findById(uid)
                    .map(IamUserEntity::getDisplayName).orElse("?");
        }
        return "?";
    }
}