package com.cathay.coindesk.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency")
public class Currency {

    // 幣別代碼本身是穩定且唯一的自然鍵，不另外配置代理主鍵。
    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "chinese_name", length = 50, nullable = false)
    private String chineseName;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    protected Currency() {
    }

    public Currency(String code, String chineseName) {
        this.code = code;
        this.chineseName = chineseName;
    }

    // 時間戳記由 JPA lifecycle callback 統一維護，不交給呼叫端自己 set，
    // 避免將來多一條寫入路徑就漏掉一次。
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdTime = now;
        updatedTime = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    public String getCode() {
        return code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
}
