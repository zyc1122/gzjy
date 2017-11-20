package com.gzjy.common.util;

import java.io.File;
import java.io.IOException;

/*import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;*/

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

public class ExcelToPdf {

	/**
	 * window环境下
	 * 
	 * @param inFilePath
	 * @param outFilePath
	 */
	public static void xlsToPdf(String inFilePath, String outFilePath) {
		File file = new File(outFilePath);
		file.getParentFile().mkdirs();
		ActiveXComponent ax = null;
		Dispatch excel = null;
		try {
			ComThread.InitSTA();
			ax = new ActiveXComponent("Excel.Application");
			ax.setProperty("Visible", new Variant(false));
			ax.setProperty("AutomationSecurity", new Variant(3)); // 禁用宏
			Dispatch excels = ax.getProperty("Workbooks").toDispatch();
			Object[] obj = new Object[] { inFilePath, new Variant(false), new Variant(false) };
			excel = Dispatch.invoke(excels, "Open", Dispatch.Method, obj, new int[9]).toDispatch();
			// 转换格式
			Object[] obj2 = new Object[] { new Variant(0), // PDF格式=0
					outFilePath, new Variant(0) // 0=标准 (生成的PDF图片不会变模糊) ; 1=最小文件
			};
			Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method, obj2, new int[1]);
		} catch (Exception es) {
			es.printStackTrace();
		} finally {
			if (excel != null) {
				Dispatch.call(excel, "Close", new Variant(false));
			}
			if (ax != null) {
				ax.invoke("Quit", new Variant[] {});
				ax = null;
			}
			ComThread.Release();
		}
	}

	/**
	 * Linux环境下
	 * 前提条件：Linux系统装好了libreoffice
	 * @param inFilePath
	 * @param outFilePath
	 */
	public static void xlsToPdfForLinux(String inFilePath, String outFilePath) {
		
		String command = "/opt/libreoffice5.4/program/soffice --headless --convert-to pdf " + inFilePath +"" +outFilePath;
        try{
            Runtime.getRuntime().exec(command);            
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
