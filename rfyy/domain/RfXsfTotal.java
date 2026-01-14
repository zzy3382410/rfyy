package com.current.rfyy.domain;

import lombok.Data;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:53
 * @Description: TODO
 **/
@Data
public class RfXsfTotal {

    /** 供应商名称 */
    private String xfmc;

    /** 销方识别号 */
    private String xfsbh;

    /** 发票数量 */
    private Integer fpCount;

    /** 发票总金额 */
    private String fpTotalAmount;

    /** 采购单数量 */
    private Integer cgdCount;

    /** 采购单总金额 */
    private String cgdTotalAmount;

    /** 日期期起 */
    private String qq;

    /** 日期期止 */
    private String qz;





}
