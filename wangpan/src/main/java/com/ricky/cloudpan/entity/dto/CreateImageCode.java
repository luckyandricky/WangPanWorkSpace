package com.ricky.cloudpan.entity.dto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.Random;
import java.io.IOException;
import java.io.OutputStream;

public class CreateImageCode {
    private int width = 160;  // 验证码图片宽度
    private int height = 40;  // 验证码图片高度
    private int codeCount = 4;   // 验证码字符个数

    //干扰线数
    private int lineCount = 20;

    // 生成随机数
    Random random = new Random();
    private BufferedImage buffImg = null;
    public String code = null;

    public String getCode() {
        return code.toLowerCase();
    }

    public CreateImageCode() {
        createImage();
    }

    public CreateImageCode(int width, int height) {
        this.width = width;
        this.height = height;
        createImage();
    }

    public CreateImageCode(int width, int height, int codeCount) {
        this.width = width;
        this.height = height;
        this.codeCount = codeCount;
        createImage();
    }

    public CreateImageCode(int width, int height, int codeCount, int lineCount) {
        this.width = width;
        this.height = height;
        this.codeCount = codeCount;
        this.lineCount = lineCount;
        createImage();
    }

    private void createImage() {
        int fontWidth = width / codeCount; //字体宽度
        int fontHeith = height -5;
        int codeY = height - 0;

        //图像buffer
        buffImg = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
        // 获取绘制验证码图片的 Graphics 对象
        Graphics g = buffImg.getGraphics();
        g.setColor(getRandColor(200,255));
        g.fillRect(0, 0, width, height);
        //设置字体
        Font font = new Font("Fixedsus",Font.BOLD,fontHeith);
        g.setFont(font);

        //设置干扰线
        for(int i = 0; i< lineCount; i++){
            int xs = random.nextInt(width);
            int ys = random.nextInt(height);
            int xe = xs + random.nextInt(width);
            int ye = ys + random.nextInt(height);
            g.setColor(getRandColor(1,255));
            g.drawLine(xs,ys,xe,ye);
        }

        //添加噪点
        float yawRate = 0.01f;
        int area = (int) (yawRate + width + height);
        for (int i = 0; i < area; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            buffImg.setRGB(x,y,random.nextInt(255));
        }

        String str1 = randomStr(codeCount);
        this.code = str1;
        for (int i = 0; i < codeCount; i++) {
            String strRand = str1.substring(i,i+1);
            g.setColor(getRandColor(1,255));
            g.drawString(strRand,i * fontWidth + 3, codeY);
        }

    }

    private String randomStr(int n){
        String str1 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String str2 = "";
        int len = str1.length() - 1;
        double r;
        for (int i = 0; i < n; i++) {
            r = (Math.random()) * len;
            str2 = str2 + str1.charAt((int) r);
        }
        return str2;
    }

    private Color getRandColor(int fc, int bc){
        if(fc >255) fc = 255;
        if(bc >255) bc = 255;
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r,g,b);
    }

    public void write(OutputStream sos) throws IOException{
        ImageIO.write(buffImg,"png",sos);
        sos.close();
    }
    public void write2(FileOutputStream sos) throws IOException{
        ImageIO.write(buffImg,"png",sos);
        sos.close();
    }

}
