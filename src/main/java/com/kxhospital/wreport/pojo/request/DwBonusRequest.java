package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DwBonusRequest {
    private Long recordId;
    /** 'publication' | 'competition' */
    private String bonusType;
    private String pubName;
    /** 'book_guide_consensus' | 'standard_norm' */
    private String pubCategory;
    private LocalDate pubDate;
    private String compName;
    /** 'provincial_joint' | 'other' */
    private String compSponsor;
    private LocalDate compDate;
}
