package com.parkonwheels.core;
import java.util.Set;
public final class TravelRewardPolicyTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if(!ok) throw new AssertionError("Travel check " + checks); }
    public static void main(String[] args) {
        TravelRewardPolicy p = new TravelRewardPolicy();
        check(!p.isCharging(0)); p.charging(null,0); check(!p.isCharging(1));
        p.charging(false,0); check(!p.isCharging(1));
        p.charging(true,100); check(p.isCharging(100)); check(!p.isCharging(99));
        check(p.isCharging(10100)); check(!p.isCharging(10101));
        p.arrived("arrival-1",100); check(p.claimable(false,200,Set.of())==null);
        check("arrival-1".equals(p.claimable(true,200,Set.of())));
        check(p.claimable(true,200,Set.of("arrival-1"))==null);
        // A duplicate callback must not extend the claim deadline.
        p.arrived("arrival-1",500000); check(p.claimable(true,600101,Set.of())==null);
        p.arrived("arrival-2",700000); check(p.claimable(true,699999,Set.of())==null);
        check("arrival-2".equals(p.claimable(true,700000,Set.of("arrival-1"))));
        p.clearArrival(); check(p.claimable(true,700001,Set.of())==null);
        p.charging(true,1);p.arrived("arrival-3",1);p.reset();check(!p.isCharging(2));check(p.claimable(true,2,Set.of())==null);
        // Persisted IDs protect against a repeated event after a process restart.
        TravelRewardPolicy restarted=new TravelRewardPolicy(); restarted.arrived("arrival-1",10);
        check(restarted.claimable(true,11,Set.of("arrival-1"))==null);
        System.out.println("PASS: " + checks + " travel policy assertions");
    }
}
