/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */     
package Controller;

import Model.CTDonHangModel;
import Model.DonHangModel;
import Model.MessagesModel;
import Model.PhiShipModel;
import Model.SachModel;
import Utility.MyUtils;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import org.json.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author TamTorres
 */
@WebServlet(name = "CheckoutServlet", urlPatterns = {"/checkout"})
public class CheckoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
  
        
        Connection conn = MyUtils.getStoredConnection(req);
        boolean isFailed = false; 

        String noiDungThongBao = "";
        
        String button = req.getParameter("submit");

        List<CTDonHangModel> listCTDonHang = new ArrayList<>();
        List<SachModel> listSachHetHang = new ArrayList<>();
        boolean isValidDonHang=true;
        HttpSession session = ((HttpServletRequest) req).getSession();
        
        int maThanhVien = MyUtils.getLoginedThanhVien(session).getMaThanhVien();      
     
                
        if (button != null && button.equals("them")) {
            try {
            
                int maDonHang = DonHangModel.getMaDonHangCurrent(conn);
                
                String diaChi = (String) req.getParameter("address");
                String soDienThoai = (String) req.getParameter("tel");
                String ghiChu = (String) req.getParameter("comment");
                Integer maPhiShip = Integer.parseInt(req.getParameter("maphiship"));               
                PhiShipModel phishipModel = PhiShipModel.FindByMaPhiShip(conn, maPhiShip);
                Double phiShip = phishipModel.getPhiShip();
                        
                String listCT = (String) req.getParameter("listctdonhang");
                JSONArray jsonListCT = new JSONArray(listCT);
              
                Double tongTien=0.0;
                
                try {
                    //JSONArray jsonArr = new JSONArray("[{\"id\":\"61\",\"price\":\"15700.0\",\"qty\":\"1\",\"name\":\"Kẻ may mắn\"},{\"id\":\"52\",\"price\":\"25000.0\",\"qty\":\"1\",\"name\":\"Sách số 123\"}]");
                    
                    for (int i = 0; i < jsonListCT.length(); i++) {
                        JSONObject jsonObj = jsonListCT.getJSONObject(i);
                        
                        SachModel sachModel = SachModel.FindByMaSach(conn, Integer.parseInt(jsonObj.getString("id")));
                        listCTDonHang.add(new CTDonHangModel(0,maDonHang,Integer.parseInt(jsonObj.getString("id")),Integer.parseInt(jsonObj.getString("qty")),
                                            sachModel.getGiaBan(), sachModel.getPhanTramGiamGia()));                       
                        
                        //listCTDonHang.add(new CTDonHangModel(0,maDonHang,Integer.parseInt(jsonObj.getString("id")),Integer.parseInt(jsonObj.getString("qty")),Double.parseDouble(jsonObj.getString("price")),0));                       
//                      System.out.println(jsonObj);
//                      System.out.println(jsonObj.getString("id"));

                        //tongTien+= Double.parseDouble(jsonObj.getString("price"))*Integer.parseInt(jsonObj.getString("qty"));
                        tongTien+= ( sachModel.getGiaBan()-  sachModel.getGiaBan()*sachModel.getPhanTramGiamGia()/100 )*Integer.parseInt(jsonObj.getString("qty"));
                    }
    
                } catch (JSONException ex) {
                   
                    ex.printStackTrace();
                };
                
                Date date = new Date(); //lấy ngày hiện tại
                
                conn.setAutoCommit(false);
                
                boolean isOKDonHang = DonHangModel.InsertDonHang(conn, new DonHangModel(
                        0,
                        maThanhVien,
                        new java.sql.Date(date.getTime()),
                        tongTien+phiShip,
                        1,
                        diaChi,
                        maPhiShip,
                        phiShip,
                        soDienThoai,
                        ghiChu        
                ));
                // conn.commit();
                if (isOKDonHang == false) {
                    throw new Exception("Thêm đơn hàng thất bại!");
                }

                /* Kiểm tra-Cập nhật số lượng tồn của sách */
                for (int i = 0; i < jsonListCT.length(); i++) {
                    JSONObject jsonObj = jsonListCT.getJSONObject(i);

                    SachModel sach = SachModel.FindByMaSach(conn, Integer.parseInt(jsonObj.getString("id")));
                    if ((sach.getSoLuongTon() - Integer.parseInt(jsonObj.getString("qty"))) < 1) {
                        System.out.print("abccccccccccc");
                        listSachHetHang.add(sach);
                        isValidDonHang = false;
                    } else {
                        sach.setSoLuongTon(sach.getSoLuongTon() - Integer.parseInt(jsonObj.getString("qty")));
                        SachModel.UpdateSach(conn, sach);
                    }
                }
                /* Kiểm tra-Cập nhật số lượng tồn của sách */
               
                
                
                /* Xử lý đơn hàng khi sản phầm hết hàng */
                if(isValidDonHang == false){
                    req.setAttribute("listSachHetHang", listSachHetHang);
                    List<PhiShipModel> listPhiShip = PhiShipModel.getAllPhiShip(conn);
                    req.setAttribute("listPhiShip", listPhiShip);
        
                    req.getRequestDispatcher("checkout.jsp").forward(req, resp);           
                    //throw new Exception("Đơn hàng không hợp lệ!");
                    
                }
                /* Xử lý đơn hàng khi sản phầm hết hàng */
                
             
                boolean isOkCTDonHang = CTDonHangModel.InsertList(conn, listCTDonHang);
                if (isOkCTDonHang == false) {
                    throw new Exception("Thêm chi tiết đơn hàng thất bại!");
                }
                
                conn.commit();
                
                isFailed = false;
                noiDungThongBao = "Đã thêm đơn hàng mới!";
                
            } catch (Exception ex) {

                try {
                    conn.rollback();
                } catch (SQLException ex1) {
                    Logger.getLogger(CheckoutServlet.class.getName()).log(Level.SEVERE, null, ex1);
                }

                isFailed = true;
                noiDungThongBao = ex.getMessage();
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    Logger.getLogger(CheckoutServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (isFailed) {
            req.setAttribute(MessagesModel.ATT_STORE, new MessagesModel("Có lỗi xảy ra!", "Yêu cầu của bạn không được xử lý!", MessagesModel.ATT_TYPE_ERROR));
            
        } else {
            req.setAttribute(MessagesModel.ATT_STORE, new MessagesModel("Thông báo!", noiDungThongBao, MessagesModel.ATT_TYPE_SUCCESS));
            req.getRequestDispatcher("trangthaisaudathang.jsp").forward(req, resp);
            
            req.setAttribute("txtTitle", "Trang chủ - Book Store");

            List<SachModel> listSachMoiNhat = SachModel.getListSachMoiNhatTop7(conn);
            SachModel objectOne = listSachMoiNhat.get(0);
            listSachMoiNhat.remove(0);
            req.setAttribute("listSachMoiNhat", listSachMoiNhat);
            req.setAttribute("sachMoiNhat", objectOne);
            List<SachModel> listSachGiamGia = SachModel.getListSachGiamGiaTop7(conn);
            SachModel objectGiamGiaNhat = listSachGiamGia.get(0);
            listSachGiamGia.remove(0);
            req.setAttribute("sachGiamGiaNhat", objectGiamGiaNhat);
            req.setAttribute("listSachGiamGia", listSachGiamGia);

            req.getRequestDispatcher("home.jsp").forward(req, resp);
        }
     
    }

   @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("txtTitle", "Checkout");
    
        Connection conn = MyUtils.getStoredConnection(req);
         
        List<PhiShipModel> listPhiShip = PhiShipModel.getAllPhiShip(conn);
        req.setAttribute("listPhiShip", listPhiShip);
        
        
        HttpSession session = req.getSession();
        if (MyUtils.getLoginedThanhVien(session) == null) // chưa đăng nhập
        {
            req.setAttribute(MessagesModel.ATT_STORE, new MessagesModel("Oops!", "Đăng nhập để tiếp tục mua hàng!", MessagesModel.ATT_TYPE_ERROR));
            req.getRequestDispatcher("giohang.jsp").forward(req, resp);
        } else {

            req.getRequestDispatcher("checkout.jsp").forward(req, resp);
        }
        //req.getRequestDispatcher("checkout.jsp").forward(req, resp);
        

    }

}
