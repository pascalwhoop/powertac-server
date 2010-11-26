// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.powertac.server.core.domain;

import java.util.List;
import java.util.Random;
import org.powertac.server.core.domain.CashPosition;
import org.springframework.stereotype.Component;

privileged aspect CashPositionDataOnDemand_Roo_DataOnDemand {
    
    declare @type: CashPositionDataOnDemand: @Component;
    
    private Random CashPositionDataOnDemand.rnd = new java.security.SecureRandom();
    
    private List<CashPosition> CashPositionDataOnDemand.data;
    
    public CashPosition CashPositionDataOnDemand.getNewTransientCashPosition(int index) {
        org.powertac.server.core.domain.CashPosition obj = new org.powertac.server.core.domain.CashPosition();
        obj.setAmount(new java.math.BigDecimal(index));
        obj.setBalance(new java.math.BigDecimal(index));
        obj.setBroker(null);
        obj.setCompetition(null);
        obj.setDateCreated(null);
        obj.setDescription("description_" + index);
        obj.setLatest(null);
        obj.setTransactionId(new Integer(index).longValue());
        return obj;
    }
    
    public CashPosition CashPositionDataOnDemand.getSpecificCashPosition(int index) {
        init();
        if (index < 0) index = 0;
        if (index > (data.size() - 1)) index = data.size() - 1;
        CashPosition obj = data.get(index);
        return CashPosition.findCashPosition(obj.getId());
    }
    
    public CashPosition CashPositionDataOnDemand.getRandomCashPosition() {
        init();
        CashPosition obj = data.get(rnd.nextInt(data.size()));
        return CashPosition.findCashPosition(obj.getId());
    }
    
    public boolean CashPositionDataOnDemand.modifyCashPosition(CashPosition obj) {
        return false;
    }
    
    public void CashPositionDataOnDemand.init() {
        data = org.powertac.server.core.domain.CashPosition.findCashPositionEntries(0, 10);
        if (data == null) throw new IllegalStateException("Find entries implementation for 'CashPosition' illegally returned null");
        if (!data.isEmpty()) {
            return;
        }
        
        data = new java.util.ArrayList<org.powertac.server.core.domain.CashPosition>();
        for (int i = 0; i < 10; i++) {
            org.powertac.server.core.domain.CashPosition obj = getNewTransientCashPosition(i);
            obj.persist();
            obj.flush();
            data.add(obj);
        }
    }
    
}
