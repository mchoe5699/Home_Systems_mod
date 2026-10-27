package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DeviceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int IDLE = 0, OUTGOING = 1, RINGING = 2, CONNECTED = 3;
    public static final int GUARD_PRESENT=0,GUARD_AWAY=1,GUARD_OUT=2;
    private String number = "", target = "", peerNumber = "";
    private UUID owner;
    private int communicationType, panelPage;
    private GlobalPos monitorLocation;
    private long monitorUntil;
    private int callState = IDLE, guardNumber, buildingNumber, unitNumber;
    private boolean addressConfigured, peerVideo;
    private int guardPresence=GUARD_PRESENT;
    private boolean bypassAllowed;
    private GlobalPos peerLocation;
    private final List<GlobalPos> ringingTargets = new ArrayList<>();
    private long callStarted, lastAction = -20;
    private final HomeNetwork homeNetwork = new HomeNetwork(this);
    private final DeviceLinks links=new DeviceLinks(this);

    public DeviceBlockEntity(BlockPos pos, BlockState state) { super(HomeSystems.DEVICE.get(), pos, state); }
    public int communicationType(){return communicationType;}
    public int panelPage(){return panelPage;}
    public boolean hasHandset(){return ProductDesign.of(getBlockState()).handset;}
    public boolean offHook(){return hasHandset()&&getBlockState().getValue(DeviceBlock.OFFHOOK);}
    public boolean setCommunicationType(int type){
        if(type<0||type>1||callState!=IDLE||!(level instanceof ServerLevel))return false;
        monitorLocation=null;communicationType=type;if(addressConfigured)DeviceDirectory.get(((ServerLevel)level).getServer()).registerAddress(this);changed();return true;
    }
    public boolean setHandset(boolean lifted){
        if(!hasHandset()||!(level instanceof ServerLevel))return false;
        level.setBlock(worldPosition,getBlockState().setValue(DeviceBlock.OFFHOOK,lifted),3);
        if(!lifted)endCall();else if(callState==RINGING)answer();changed();return true;
    }
    public DeviceBlockEntity monitorSource(){
        if(!(level instanceof ServerLevel server)||monitorLocation==null||level.getGameTime()>monitorUntil||callState!=IDLE)return null;
        var source=DeviceDirectory.resolve(server.getServer(),monitorLocation);
        return source!=null&&source.getBlockState().is(HomeSystems.DOOR_CAMERA.get())&&source.links().isLinked(this)&&DeviceRouting.allowed(source,this)?source:null;
    }
    public boolean monitoring(){return monitorLocation!=null&&level!=null&&level.getGameTime()<=monitorUntil&&callState==IDLE;}
    public boolean startMonitor(){
        if(!(level instanceof ServerLevel server)||!DeviceVideo.hasDisplay(this)||!DeviceRouting.indoor(this)||callState!=IDLE)return false;
        var source=DeviceDirectory.get(server.getServer()).cameraFor(this);if(source==null)return false;
        monitorLocation=source.location();monitorUntil=level.getGameTime()+600;panelPage=0;changed();return true;
    }
    public String number() { return addressConfigured ? (DeviceRouting.guard(this) ? "G" + guardNumber : buildingNumber + "-" + unitNumber) : number; }
    public String legacyNumber() { return number; }
    public String target() { return target; }
    public String peerNumber() { return peerNumber; }
    public int state() { return callState; }
    public boolean addressConfigured() { return addressConfigured; }
    public int guardNumber() { return guardNumber; }
    public int buildingNumber() { return buildingNumber; }
    public int unitNumber() { return unitNumber; }
    public boolean peerHasVideo() { return peerVideo; }
    public int guardPresence(){return guardPresence;}
    public boolean canCallAlternateGuard(){return level instanceof ServerLevel serverLevel?DeviceDirectory.get(serverLevel.getServer()).canBypass(this):bypassAllowed;}
    public boolean setGuardPresence(int presence){
        if(!(level instanceof ServerLevel serverLevel)||!DeviceRouting.guard(this)||!addressConfigured||presence<0||presence>2)return false;
        if(presence!=GUARD_PRESENT)endCall();guardPresence=presence;
        var directory=DeviceDirectory.get(serverLevel.getServer());directory.registerAddress(this);directory.refreshPresenceClients(serverLevel.getServer());changed();return true;
    }
    public boolean ready() { return addressConfigured || !number.isEmpty(); }
    public HomeNetwork homeNetwork() { return homeNetwork; }
    public DeviceLinks links(){return links;}
    public GlobalPos location() { return GlobalPos.of(level.dimension(), worldPosition); }
    public void setOwner(UUID uuid) { owner = uuid; changed(); }
    public boolean canConfigure(Player player) { return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2); }
    public DeviceBlockEntity peer() { return level instanceof ServerLevel serverLevel ? DeviceDirectory.resolve(serverLevel.getServer(), peerLocation) : null; }
    public Component addressLabel() {
        if (!addressConfigured) return Component.translatable("screen.homesystems.not_configured");
        return DeviceRouting.guard(this) ? Component.translatable("screen.homesystems.guard_address", guardNumber).append(" · ").append(Component.translatable("screen.homesystems.presence_"+guardPresence))
            : Component.translatable("screen.homesystems.home_address", buildingNumber, unitNumber, guardNumber);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            var directory = DeviceDirectory.get(serverLevel.getServer());
            if (addressConfigured) directory.registerAddress(this);
            else if (!number.isEmpty() && !directory.claim(this, number)) { number = ""; changed(); }
            if (getBlockState().getValue(DeviceBlock.POWERED)) level.scheduleTick(worldPosition, getBlockState().getBlock(), 100);
        }
    }

    /** Compatibility for old saved numeric endpoints; new GUIs only expose structured addressing. */
    public boolean configure(String ownNumber, String destination) {
        if (!(level instanceof ServerLevel serverLevel) || addressConfigured || !EstateData.inside(this) || callState != IDLE
            || !DeviceDirectory.validNumber(ownNumber) || (!destination.isEmpty() && !DeviceDirectory.validNumber(destination))) return false;
        if (!DeviceDirectory.get(serverLevel.getServer()).claim(this, ownNumber)) return false;
        number = ownNumber; target = destination; changed(); return true;
    }
    public boolean configureAddress(int guard, int building, int unit) {
        if (!(level instanceof ServerLevel serverLevel) || !EstateData.inside(this) || callState != IDLE
            || !DeviceRouting.valid(guard) || !DeviceRouting.valid(building) || !DeviceRouting.valid(unit)) return false;
        guardNumber = guard; buildingNumber = DeviceRouting.guard(this) ? 0 : building; unitNumber = DeviceRouting.guard(this) ? 0 : unit;
        addressConfigured = true; number = ""; target = "";
        DeviceDirectory.get(serverLevel.getServer()).registerAddress(this); changed(); return true;
    }
    public void saveAddress(ServerPlayer player, int guard, int building, int unit) {
        if (!EstateData.playerMayUse(player, this)) { tell(player, "outside_estate"); return; }
        if (!canConfigure(player)) { tell(player, "owner_only"); return; }
        if (level.getGameTime() - lastAction < 4) return;
        lastAction = level.getGameTime();
        tell(player, configureAddress(guard, building, unit) ? "address_saved" : "address_invalid");
    }

    public void handle(ServerPlayer player, DeviceNetwork.Request request) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (request.action() != DeviceNetwork.Action.END && !EstateData.playerMayUse(player, this)) { tell(player, "outside_estate"); return; }
        long now = level.getGameTime(); if (now - lastAction < 4) return; lastAction = now;
        var directory = DeviceDirectory.get(serverLevel.getServer());
        switch (request.action()) {
            case CALL, CALL_GUARD, CALL_OTHER_GUARD -> {
                if (!addressConfigured) { tell(player, "configure_first"); return; }
                List<DeviceBlockEntity> receivers;
                if (request.action() == DeviceNetwork.Action.CALL) {
                    if(DeviceRouting.door(this)&&links.homeCount()==0){tell(player,"home_link_required");return;}
                    int b = DeviceRouting.door(this) ? buildingNumber : request.building();
                    int u = DeviceRouting.door(this) ? unitNumber : request.unit();
                    if (!DeviceRouting.valid(b) || !DeviceRouting.valid(u)) { tell(player, "address_invalid"); return; }
                    receivers = directory.household(this, b, u);
                } else {
                    int g = request.action() == DeviceNetwork.Action.CALL_GUARD ? guardNumber : request.guard();
                    if (!DeviceRouting.valid(g)) { tell(player, "address_invalid"); return; }
                    if(g!=guardNumber&&!directory.canBypass(this)){tell(player,"bypass_forbidden");return;}
                    receivers = directory.guards(this, g, request.action() == DeviceNetwork.Action.CALL_GUARD);
                }
                tell(player, callMany(receivers) ? "calling" : "address_offline");
            }
            case ANSWER -> { if (!answer()) tell(player, "no_call"); }
            case END -> {monitorLocation=null;endCall();changed();}
            case HANDSET -> setHandset(!offHook());
            case MONITOR -> {if(!canConfigure(player)){tell(player,"owner_only");return;}if(monitoring()){monitorLocation=null;changed();}else if(!startMonitor())tell(player,"monitor_unavailable");}
            case PANEL_HOME -> {if(homeNetwork.isWallpad()){panelPage=panelPage==1?0:1;changed();}}
            case TYPE_STAR,TYPE_BUS -> {if(!canConfigure(player)){tell(player,"owner_only");return;}if(setCommunicationType(request.action()==DeviceNetwork.Action.TYPE_STAR?0:1))tell(player,"type_saved");}
            case UNLOCK -> {
                if (!canConfigure(player)) { tell(player, "owner_only"); return; }
                tell(player, openLinkedDoors() ? "unlocked" : "cannot_unlock");
            }
            case HOME_LIGHTS, HOME_DOOR, HOME_GAS, HOME_OTHER -> {
                if (!canConfigure(player)) { tell(player, "owner_only"); return; }
                int channel = switch (request.action()) { case HOME_LIGHTS -> 0; case HOME_DOOR -> 1; case HOME_GAS -> 2; default -> 3; };
                if (!homeNetwork.toggle(channel)) tell(player, "no_relays");
            }
            case GUARD_PRESENT,GUARD_AWAY,GUARD_OUT->{
                if(!canConfigure(player)){tell(player,"owner_only");return;}
                int presence=switch(request.action()){case GUARD_AWAY->1;case GUARD_OUT->2;default->0;};
                if(setGuardPresence(presence))tell(player,"presence_saved");
            }
        }
    }

    public boolean call(DeviceBlockEntity receiver) { return receiver != null && callMany(List.of(receiver)); }
    public boolean callMany(List<DeviceBlockEntity> receivers) {
        if (!(level instanceof ServerLevel) || callState != IDLE || !ready()) return false;
        var candidates = receivers.stream().filter(Objects::nonNull).filter(d -> d.ready() && d.callState == IDLE && DeviceRouting.allowed(this, d)).distinct().limit(64).toList();
        if (candidates.isEmpty()) return false;
        ringingTargets.clear();
        for (DeviceBlockEntity receiver : candidates) {
            ringingTargets.add(receiver.location()); receiver.peerLocation = location(); receiver.peerNumber = number();
            receiver.peerVideo = DeviceRouting.videoSource(this); receiver.callState = RINGING; receiver.callStarted = receiver.level.getGameTime();
            receiver.changed(); receiver.notifyNearby(Component.translatable("message.homesystems.incoming", number())); receiver.ring();
        }
        peerLocation = candidates.get(0).location(); peerNumber = candidates.get(0).number(); peerVideo = DeviceRouting.videoSource(candidates.get(0));
        callState = OUTGOING; callStarted = level.getGameTime(); changed(); return true;
    }
    public boolean answer() {
        DeviceBlockEntity caller = peer();
        if (callState != RINGING || caller == null || caller.callState != OUTGOING || !caller.ringingTargets.contains(location()) || !DeviceRouting.allowed(caller, this)) return false;
        for (GlobalPos pos : List.copyOf(caller.ringingTargets)) {
            if (pos.equals(location())) continue;
            var other = DeviceDirectory.resolve(((ServerLevel)level).getServer(), pos);
            if (other != null && other.callState == RINGING && caller.location().equals(other.peerLocation)) other.clearCall();
        }
        caller.ringingTargets.clear(); caller.peerLocation = location(); caller.peerNumber = number(); caller.peerVideo = DeviceRouting.videoSource(this);
        callState = caller.callState = CONNECTED; if(hasHandset())level.setBlock(worldPosition,getBlockState().setValue(DeviceBlock.OFFHOOK,true),3); callStarted = level.getGameTime(); caller.callStarted = caller.level.getGameTime();
        changed(); caller.changed();
        notifyNearby(Component.translatable("message.homesystems.connected", caller.number()));
        caller.notifyNearby(Component.translatable("message.homesystems.connected", number())); return true;
    }
    private void refreshOutgoing() {
        if (callState != OUTGOING || !(level instanceof ServerLevel serverLevel)) return;
        ringingTargets.removeIf(pos -> {
            var d = DeviceDirectory.resolve(serverLevel.getServer(), pos);
            boolean invalid = d == null || d.callState != RINGING || !location().equals(d.peerLocation) || !DeviceRouting.allowed(this, d);
            if (invalid && d != null && d.callState == RINGING && location().equals(d.peerLocation)) d.clearCall();
            return invalid;
        });
        if (ringingTargets.isEmpty()) { clearCall(); return; }
        if (!ringingTargets.contains(peerLocation)) {
            peerLocation = ringingTargets.get(0); var d = peer(); peerNumber = d.number(); peerVideo = DeviceRouting.videoSource(d); changed();
        }
    }
    public void endCall() {
        DeviceBlockEntity other = peer();
        if (callState == OUTGOING && level instanceof ServerLevel serverLevel) {
            for (GlobalPos pos : List.copyOf(ringingTargets)) {
                var d = DeviceDirectory.resolve(serverLevel.getServer(), pos);
                if (d != null && d.callState == RINGING && location().equals(d.peerLocation)) d.clearCall();
            }
        } else if (callState == RINGING && other != null && other.callState == OUTGOING) {
            other.ringingTargets.remove(location()); clearCall(); other.refreshOutgoing(); return;
        } else if (other != null && location().equals(other.peerLocation)) other.clearCall();
        clearCall();
    }
    private void clearCall() {
        if (callState == IDLE && peerLocation == null && ringingTargets.isEmpty()) return;
        callState = IDLE; peerLocation = null; peerNumber = ""; peerVideo = false; ringingTargets.clear(); changed();
    }
    public boolean unlockPeer() {
        DeviceBlockEntity other = peer();
        if (callState != CONNECTED || other == null || !DeviceRouting.allowed(this, other) || other.callState != CONNECTED || !location().equals(other.peerLocation)
            || !DeviceRouting.entrance(other)) return false;
        BlockState state = other.getBlockState(); if (state.getValue(DeviceBlock.POWERED)) return false;
        other.level.setBlock(other.worldPosition, state.setValue(DeviceBlock.POWERED, true), 3);
        other.level.updateNeighborsAt(other.worldPosition, state.getBlock()); other.level.scheduleTick(other.worldPosition, state.getBlock(), 100);
        other.level.playSound(null, other.worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, .7F, 1F); return true;
    }
    public boolean openLinkedDoors(){
        if(!(level instanceof ServerLevel)||!EstateData.inside(this))return false;
        var other=peer();
        if(callState==CONNECTED&&other!=null&&DeviceRouting.allowed(this,other)){
            if(DeviceRouting.lobby(other)){boolean opened=other.links.openDoors()>0;return unlockPeer()||opened;}
            if(DeviceRouting.door(other)){int opened=links.openDoors();return unlockPeer()||opened>0;}
        }
        return links.openDoors()>0;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, DeviceBlockEntity d) {
        if (d.callState == OUTGOING) d.refreshOutgoing();
        var other = d.peer();
        if (d.callState == RINGING && (other == null || other.callState != OUTGOING || !other.ringingTargets.contains(d.location()) || !DeviceRouting.allowed(other, d))) d.endCall();
        else if (d.callState == CONNECTED && (other == null || other.callState != CONNECTED || !d.location().equals(other.peerLocation) || !DeviceRouting.allowed(d, other))) d.endCall();
        if (!EstateData.inside(d) && state.getValue(DeviceBlock.POWERED)) { level.setBlock(pos, state.setValue(DeviceBlock.POWERED, false), 3); level.updateNeighborsAt(pos, state.getBlock()); }
        if (level.getGameTime() % 20 == 0) {
            d.homeNetwork.tick();d.links.tick();if(d.monitorLocation!=null&&d.monitorSource()==null){d.monitorLocation=null;d.changed();}boolean bypass=d.canCallAlternateGuard();if(bypass!=d.bypassAllowed){d.bypassAllowed=bypass;d.changed();}
        }
        if (level.getGameTime() % 5 == 0) DeviceVideo.tick(d);
        if (d.callState == IDLE || level.getGameTime() % 20 != 0) return;
        if (level.getGameTime() - d.callStarted >= (d.callState == CONNECTED ? 6000 : 600)) { d.endCall(); return; }
        if (d.callState == RINGING && level.getGameTime() % 40 == 0) d.ring();
    }
    private void ring() { level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, .9F, 1.15F); }
    private static void tell(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable("message.homesystems." + key)); }
    public void notifyNearby(Component message) {
        if (level instanceof ServerLevel serverLevel) serverLevel.players().stream().filter(player -> EstateData.playerMayUse(player, this)
            && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 256).forEach(player -> player.sendSystemMessage(message));
    }
    void changed() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("CommunicationType",communicationType);tag.putInt("PanelPage",panelPage); tag.putString("Number", number); tag.putString("Target", target); if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("AddressConfigured", addressConfigured); tag.putInt("GuardNumber", guardNumber); tag.putInt("BuildingNumber", buildingNumber); tag.putInt("UnitNumber", unitNumber);tag.putInt("GuardPresence",guardPresence); homeNetwork.save(tag);links.save(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); communicationType=tag.getInt("CommunicationType")==1?1:0;panelPage=tag.getInt("PanelPage")==1?1:0;monitorLocation=null;monitorUntil=0;
        if(tag.contains("MonitorUntil")){monitorUntil=tag.getLong("MonitorUntil");if(tag.contains("MonitorPos")&&level!=null)monitorLocation=GlobalPos.of(level.dimension(),BlockPos.of(tag.getLong("MonitorPos")));} number = tag.getString("Number"); target = tag.getString("Target"); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        guardNumber = tag.getInt("GuardNumber"); buildingNumber = tag.getInt("BuildingNumber"); unitNumber = tag.getInt("UnitNumber");
        guardPresence=tag.getInt("GuardPresence");if(guardPresence<0||guardPresence>2)guardPresence=GUARD_PRESENT;
        addressConfigured = tag.getBoolean("AddressConfigured") && DeviceRouting.valid(guardNumber) && DeviceRouting.valid(buildingNumber) && DeviceRouting.valid(unitNumber); homeNetwork.load(tag);links.load(tag);
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); tag.putInt("CallState", callState); tag.putString("PeerNumber", peerNumber); tag.putBoolean("PeerVideo", peerVideo);tag.putBoolean("BypassAllowed",canCallAlternateGuard());if(monitorLocation!=null){tag.putLong("MonitorPos",monitorLocation.pos().asLong());tag.putLong("MonitorUntil",monitorUntil);}return tag; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); callState = tag.getInt("CallState"); peerNumber = tag.getString("PeerNumber"); peerVideo = tag.getBoolean("PeerVideo");bypassAllowed=tag.getBoolean("BypassAllowed"); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) { if (packet.getTag() != null) handleUpdateTag(packet.getTag()); }
    @Override public Component getDisplayName() { return SiblingModels.options(getBlockState().getBlock()).stream().filter(option -> option.slot() == getBlockState().getValue(DeviceBlock.MODEL)).map(SiblingModels.Option::name).findFirst().orElse(getBlockState().getBlock().getName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new DeviceMenu(id, inventory, worldPosition); }
}
