package online.vivaseikatsu.stra.vivaportal;

import jdk.internal.jshell.tool.StopDetectingInputStream;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class PortalListener implements Listener {

    private final VivaPortal plg;

    public PortalListener(VivaPortal plg_){
        plg = plg_;
    }

    // selectコマンドによるブロック選択
    @EventHandler
    private void onPlayerInteractEvent(PlayerInteractEvent e){
        Player p = e.getPlayer();
        // コマンド制御用のハッシュマップをみる、名前があってtrueの場合のみ続行
        if(!plg.SelectCommand.containsKey(p.getName())) return;
        if(!plg.SelectCommand.get(p.getName())) return;
        // クリックしたブロックが取得できない場合は終了
        if(e.getClickedBlock() == null) return;

        // クリックしたブロックのロケーションをブロック用ハッシュマップに格納
        plg.SelectedBlock.put(p.getName(),e.getClickedBlock().getLocation());
        p.sendMessage(ChatColor.GRAY + "[vivaPortal] ブロックを選択しました。");

        // 殴ったのをなかったことに
        e.setCancelled(true);

        // コマンド制御用マップからプレイヤーを削除
        plg.SelectCommand.remove(p.getName());

    }

    // ポータルに重なったとき、ワープさせる
    @EventHandler
    private void onPlayerMoveEvent(PlayerMoveEvent e){

        // プレイヤーを取得
        Player p = e.getPlayer();

        //移動先がnullの場合(首を回したとか)は、終了
        if(e.getTo() == null) return;

        // 同じブロック内での移動だった場合、終了
        if(e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()
        ) return;

        // 権限がないプレイヤーだった場合は、終了
        if(!p.hasPermission("vivaportal.use")) return;

        // ポータルの場所を取得
        Location main2res = plg.Portal2Location("main2res");
        Location res2main = plg.Portal2Location("res2main");

        // 移動元のブロックがポータル内の場合は、終了
        if(isPortalLocation(e.getFrom(),main2res)) return;
        if(isPortalLocation(e.getFrom(),res2main)) return;

        // ポータル"main2res"と移動先ブロックがかぶったとき (メイン -> 資源)
        if(isPortalLocation(e.getTo(),main2res)){
            PlayerPortalTP(p,"res2main");
            return;
        }

        // ポータル"res2main"と移動先ブロックがかぶったとき (資源 -> メイン)
        if(isPortalLocation(e.getTo(),res2main)){
            PlayerPortalTP(p,"main2res");
            return;
        }


        // ポータル"main2res"にMobを押し込んだときの処理
        if(isNearThePortal(e.getTo(),main2res,5)){
            MobPortalTP(main2res,res2main);
            return;
        }

        // ポータル"res2main"にMobを押し込んだときの処理
        if(isNearThePortal(e.getTo(),res2main,5)){
            MobPortalTP(res2main,main2res);
            return;
        }


    // onPlayerMoveEventここまで
    }

    // 乗り物が動くときの処理
    @EventHandler
    private void onVehicleMoveEvent(VehicleMoveEvent e){

        // 乗り物を取得
        Vehicle v = e.getVehicle();

        // 乗り物に何も乗っていない場合は終了
        if(v.getPassenger() == null) return;

        // 同じブロック内での移動だった場合、終了
        if(e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()
        ) return;



        // ポータルの場所を取得
        Location main2res = plg.Portal2Location("main2res");
        Location res2main = plg.Portal2Location("res2main");

        // 移動元のブロックがポータル内の場合は、終了
        if(isPortalLocation(e.getFrom(),main2res)) return;
        if(isPortalLocation(e.getFrom(),res2main)) return;

        // ポータル"main2res"と移動先ブロックがかぶったとき (メイン -> 資源)
        if(isPortalLocation(e.getTo(),main2res)){
            VehiclePortalTP(v,"res2main");
            return;
        }

        // ポータル"res2main"と移動先ブロックがかぶったとき (資源 -> メイン)
        if(isPortalLocation(e.getTo(),res2main)){
            VehiclePortalTP(v,"main2res");
            return;
        }


    }







    // 資源ワールドの初期スポにポータルを生成
    public void SetResSpawnPortal(){

        // 資源ワールドのワールド名を取得
        String res_name = plg.config.getString("res_world_name","null");

        // 有効なワールド名じゃない場合は、終了
        if(!plg.getServer().getWorlds().toString().contains(res_name)) return;

        // 資源ワールドのスポーン座標を取得
        Location spawn = plg.getServer().getWorld(res_name).getSpawnLocation();
        // スポーン地点の足元を指定
        spawn.setY(spawn.getY() -1);

        // config.ymlに値をセット
        plg.config.set("res2main.world_name", spawn.getWorld().getName());
        plg.config.set("res2main.x", (int) spawn.getX());
        plg.config.set("res2main.y", (int) spawn.getY());
        plg.config.set("res2main.z", (int) spawn.getZ());

        // config.ymlに書き込み
        plg.saveConfig();

        // ポータルのハリボテを生成
        plg.CreateGateway(plg.Portal2Location("res2main"));

    }

    // 受け取ったロケーションがポータルの範囲内（タテ3ブロック）にあるかを返す
    public boolean isPortalLocation(Location location , Location portal){

        // ワールドが取得できない場合はfalse
        if(location.getWorld() == null) return false;
        if(portal.getWorld() == null) return false;

        // ワールドが違えば、違う
        if(location.getWorld() != portal.getWorld()) return false;
        // xが違えば、違う
        if(location.getBlockX() != portal.getBlockX()) return false;
        // zが違えば、違う
        if(location.getBlockZ() != portal.getBlockZ()) return false;

        // ここまで来れば、x,zが一致している

        // yが範囲内なら、true
        if(location.getBlockY() == portal.getBlockY()) return true;
        if(location.getBlockY() == portal.getBlockY() + 1) return true;
        if(location.getBlockY() == portal.getBlockY() + 2) return true;

        // 結局違えば、false
        return false;

    }

    // 受け取ったロケーションがポータルの近くにあるかを返す（タテ3ブロック,周囲任意のブロック数）
    public boolean isNearThePortal(Location location , Location portal , int radius){

        // ワールドが取得できない場合はfalse
        if(location.getWorld() == null) return false;
        if(portal.getWorld() == null) return false;

        // ワールドが違えば、違う
        if(location.getWorld() != portal.getWorld()) return false;

        // 指定された半径以内じゃない場合、false
        if(plg.getDistance( location.getBlockX(),
                location.getBlockZ() , portal.getBlockX(), portal.getBlockZ() ) >= radius) return false;

        // ここまで来れば、x,zが範囲内にある

        // 高さが範囲内なら、true
        if(portal.getBlockY() - 3 <= location.getBlockY()
                || location.getBlockY() <= portal.getBlockY() + 3 ) return true;


        // 結局違えば、false
        return false;

    }

    // 指定したブロックの周りを確認し、隣接して2マス分の空間がある場所を返す。なかった場合はもとのブロックを返す。
    public Location TPpointCheck(Location center){

        Location exitBlock = center.clone();

        for(int x = -1; x <= 1; x++){
            for(int z = 1; z >= -1; z--){

                // xないしzが0のときはcontinue
                if(x == 0 || z == 0) continue;

                // チェックするブロックを設定
                Location check = center.clone();
                check.setX(check.getX() + (double) x);
                check.setZ(check.getZ() + (double) z);

                Location check2 = check.clone();
                check2.setY(check.getY() + 1);

                // チェック先がタテに2マスAIRなら、隣接する場所の確認
                if(!check.getBlock().getType().isSolid()
                        && !check2.getBlock().getType().isSolid()){

                    // チェックするブロックを設定
                    check = center.clone();
                    check.setZ(check.getZ() + (double) z);

                    check2 = check.clone();
                    check2.setY(check.getY() + 1);

                    if(!check.getBlock().getType().isSolid()
                            && !check2.getBlock().getType().isSolid()){
                        exitBlock = check.clone();
                        break;
                    }

                    // チェックするブロックを設定
                    check = center.clone();
                    check.setX(check.getX() + (double) x);

                    check2 = check.clone();
                    check2.setY(check.getY() + 1);

                    if(!check.getBlock().getType().isSolid()
                            && !check2.getBlock().getType().isSolid()){
                        exitBlock = check.clone();
                        break;
                    }

                }
            }
        }

        return exitBlock;

    }

    // プレイヤーテレポートの処理
    public void PlayerPortalTP(Player player,String exitPortal){


        // 資源側のポータルがない場合、ポータルをスポーン地点に自動生成
        if(plg.config.getString("res2main.world_name") == null){
            SetResSpawnPortal();
        }

        // テレポート先が不正な場合
        if(plg.Portal2Location(exitPortal).getWorld() == null){
            player.sendMessage(ChatColor.GRAY + "[vivaPortal] "+ exitPortal +"の設定がされていません！");
            return;
        }

        // 乗り物に乗っているとき用の処理
        if(player.getVehicle() != null){
            VehiclePortalTP((Vehicle) player.getVehicle(),exitPortal);
            // ポータルのハリボテを生成
            plg.CreateGateway(plg.Portal2Location(exitPortal));
            return;
        }

        // 通常のテレポート処理
        player.teleport(plg.Portal2Location(exitPortal));
        // ポータルのハリボテを生成
        plg.CreateGateway(plg.Portal2Location(exitPortal));

    }

    // Mobのテレポートの処理
    public void MobPortalTP(Location enterPortal,Location exitPortal){

        // 相手側ポータルが不正な場合、終了
        if(exitPortal.getWorld() == null) return;



        // TP先の設定
        Location exitBlock = exitPortal.clone();

        // ポータルに重なってるMobを探して、テレポートさせる
        for(Entity entity : enterPortal.getChunk().getEntities() ){

            if(entity.getType() == EntityType.PLAYER) continue;

            if(isPortalLocation(entity.getLocation(),enterPortal)){

                // entityがなにかに乗っている場合、乗り物の処理を優先
                if(entity.getVehicle() != null) return;
                // 何かを乗せている場合、乗り物処理を優先
                if(entity.getPassenger() != null) return;

                // ポータル周りのAirブロックの場所をTP先に選定
                exitBlock = TPpointCheck(exitBlock);
                if(isPortalLocation(exitBlock,exitPortal)){
                    exitBlock.setY(exitBlock.getY() +1);
                    exitBlock = TPpointCheck(exitBlock);
                }

                // エンティティをテレポート
                entity.teleport(exitBlock);
            }

        }

    }

    // 乗り物テレポートの処理
    public void VehiclePortalTP(Vehicle vehicle,String exitPortal){


        // TP先ポータルを取得
        Location exitBlock = plg.Portal2Location(exitPortal);

        // ポータル周りのAirブロックの場所をTP先に選定
        exitBlock = TPpointCheck(exitBlock);
        if(isPortalLocation(exitBlock,plg.Portal2Location(exitPortal))){
            exitBlock.setY(exitBlock.getY() +1);
            exitBlock = TPpointCheck(exitBlock);
        }

        // 乗ってるエンティティのリストを生成
        List<Entity> entityList = new ArrayList<Entity>(vehicle.getPassengers());

        // 乗ってるエンティティをすべて下ろす
        vehicle.eject();

        // テレポート処理
        vehicle.teleport(exitBlock);

        for(Entity e : entityList){
            e.teleport(exitBlock);
            vehicle.addPassenger(e);
        }


    }


}





