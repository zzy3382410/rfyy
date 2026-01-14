package com.current.rfyy.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:39
 * @Description: TODO
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RfQueryDto {

    /** 供应商名称 */
    private String xfmc;

    /** 销方识别号 */
    private String xfsbh;

    /** 日期期起 */
    private String qq;

    /** 日期期止 */
    private String qz;



}
