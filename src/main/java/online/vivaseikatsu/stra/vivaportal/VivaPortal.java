package online.vivaseikatsu.stra.vivaportal;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class VivaPortal extends JavaPlugin {

    public FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic

        // listenerの処理を行うクラスを宣言
        getServer().getPluginManager().registerEvents(new PortalListener(this),this);

        // config.ymlの準備
        // config.ymlがない場合はファイルを出力
        saveDefaultConfig();

        // config.ymlの読み込み
        config = getConfig();

        getLogger().info("プラグインが有効になりました。");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("プラグインが無効になりました。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ポータルの作成、削除のコマンド
        if(command.getName().equalsIgnoreCase("vivaportal")){

            if(sender instanceof Player){
                if(!sender.hasPermission("vivaportal.setting")){
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] 権限がありません!");
                }
            }

            // オプションがない場合
            if(args.length == 0){
                sender.sendMessage(ChatColor.GRAY +"[vivaPortal] オプションを指定してください!");
                sender.sendMessage(ChatColor.GRAY +"[vivaPortal] /vivaportal help");
                return true;
            }

            // selectオプション
            if(args[0].equalsIgnoreCase("select")) {
                // senderがプレイヤーじゃないとき
                if(!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] プレイヤーのみが実行可能な処理です。");
                    return true;
                }
                Player p = (Player) sender;
                // セレクトコマンドを実行したフラグをたてる
                SelectCommand.put(p.getName(),true);
                // ブロックを選択するようにメッセージ
                p.sendMessage(ChatColor.GRAY + "[vivaPortal] 選択したいブロックをクリックしてください。");
                return true;
            }

            // regenオプション
            if(args[0].equalsIgnoreCase("regen")) {
                // main2resの再生成
                if(config.getString("main2res") != null){
                    RemoveGateway(Portal2Location("main2res"));
                    CreateGateway(Portal2Location("main2res"));
                }
                // res2mainの再生成
                if(config.getString("res2main") != null){
                    RemoveGateway(Portal2Location("res2main"));
                    CreateGateway(Portal2Location("res2main"));
                }

                // 再生成完了のメッセージ
                sender.sendMessage(ChatColor.GRAY + "[vivaPortal] ポータルの構造物を再生成しました。");
                return true;
            }

            // removeオプション
            if(args[0].equalsIgnoreCase("remove")){

                // 引数がないとき
                if(args.length == 1) {
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] 有効なポータルを指定してください!");
                    return true;
                }

                // 有効なポータルが指定されなかったとき
                if(!args[1].equals("main2res") &
                        !args[1].equals("res2main")){
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] 有効なポータルを指定してください!");
                    return true;
                }

                // 指定されたポータル名を格納
                String portal_name = args[1];

                // 古いポータルが存在する場合、設定を削除
                if(config.getString(portal_name + ".world_name") != null){

                    // ポータルの構造物を削除(読み込み済みの場合のみ)
                    RemoveGateway(Portal2Location(portal_name));

                    // configの内容を書き換え
                    config.set(portal_name + ".world_name", null);
                    config.set(portal_name + ".x", null);
                    config.set(portal_name + ".y", null);
                    config.set(portal_name + ".z", null);
                    config.set(portal_name + ".direction", null);
                    // config.ymlに書き込み
                    saveConfig();

                    // 削除の通知
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] " + portal_name + "を削除しました。");
                    return true;
                }

                sender.sendMessage(ChatColor.GRAY +"[vivaPortal] " + portal_name + "はすでに削除されています。");
                return true;

            // removeここまで
            }

            // setオプション
            if(args[0].equalsIgnoreCase("set")) {

                // senderがプレイヤーじゃないとき
                if(!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.GRAY +"[vivaPortal] プレイヤーのみが実行可能な処理です。");
                    return true;
                }

                Player p = (Player) sender;

                // 引数がないとき
                if(args.length < 2) {
                    p.sendMessage(ChatColor.GRAY +"[vivaPortal] 有効なポータルを指定してください!");
                    return true;
                }

                // 有効なポータルが指定されなかったとき
                if(!args[1].equals("main2res") &
                        !args[1].equals("res2main")){
                    p.sendMessage(ChatColor.GRAY +"[vivaPortal] 有効なポータルを指定してください!");
                    return true;
                }

                // ブロック選択がされていないとき
                if(!SelectedBlock.containsKey(p.getName())){
                    p.sendMessage(ChatColor.GRAY +"[vivaPortal] ポータルを設置するブロックを指定してください!");
                    return true;
                }
                if(SelectedBlock.get(p.getName()) == null){
                    p.sendMessage(ChatColor.GRAY +"[vivaPortal] ポータルを設置するブロックを指定してください!");
                    return true;
                }


                // 指定されたポータル名を格納
                String portal_name = args[1];

                // 古いポータルが存在する場合、その構造を削除(読み込まれているときのみ)
                RemoveGateway(Portal2Location(portal_name));

                // 選択済みのブロックのロケーションを取得
                Location l_new = SelectedBlock.get(p.getName());

                // config.ymlに値をセット
                config.set(portal_name + ".world_name", l_new.getWorld().getName());
                config.set(portal_name + ".x", (int) l_new.getX());
                config.set(portal_name + ".y", (int) l_new.getY());
                config.set(portal_name + ".z", (int) l_new.getZ());

                // args[2] が参照できるときのみ参照する
                if(args.length >= 3) {
                    if(args[2].equalsIgnoreCase("n")){
                        config.set(portal_name + ".direction", 180);
                    }
                    if(args[2].equalsIgnoreCase("s")){
                        config.set(portal_name + ".direction", 0);
                    }
                    if(args[2].equalsIgnoreCase("e")){
                        config.set(portal_name + ".direction", 270);
                    }
                    if(args[2].equalsIgnoreCase("w")){
                        config.set(portal_name + ".direction", 90);
                    }
                }

                // config.ymlに書き込み
                saveConfig();

                // ポータルのハリボテを生成
                CreateGateway(Portal2Location(portal_name));

                // 選択したブロックを忘れる
                SelectedBlock.remove(p.getName());

                p.sendMessage(ChatColor.GRAY + "[vivaPortal] " + portal_name + "の設定が完了しました。");

                return true;

            // setオプションここまで
            }


            // 有効なオプションがしていされなかったとき or Help
            if(args[0].equalsIgnoreCase("help")){
                sender.sendMessage(ChatColor.GRAY +"[vivaPortal] Help");
            }else{
                sender.sendMessage(ChatColor.GRAY +"[vivaPortal] 有効なオプションを指定してください!");
            }
            sender.sendMessage(ChatColor.GRAY +"/vivaportal select");
            sender.sendMessage(ChatColor.GRAY +"/vivaportal set <main2res/res2main> <n,s,e,w>");
            sender.sendMessage(ChatColor.GRAY +"/vivaportal regen");
            sender.sendMessage(ChatColor.GRAY +"/vivaportal remove <main2res/res2main>");
            return true;

        // vivaPortalここまで
        }


        return true;
    // onCommandここまで
    }



    // コマンドのTab補完
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(command.getName().equalsIgnoreCase("vivaportal")){

            // Tab補完用のリストを作成
            List<String> argList = new ArrayList<>();

            // 引数1つ目の処理
            if (args.length == 1) {

                argList.add("select");
                argList.add("set");
                argList.add("regen");
                argList.add("remove");
                argList.add("help");

                return argList.stream().filter(a -> a.startsWith(args[0])).collect(Collectors.toList());
            // 1つ目ここまで
            }

            // 引数2つ目の処理
            if (args.length == 2) {

                if(args[0].equalsIgnoreCase("set")
                        || args[0].equalsIgnoreCase("remove")){

                    argList.add("main2res");
                    argList.add("res2main");

                }

                return argList.stream().filter(a -> a.startsWith(args[1])).collect(Collectors.toList());
            // 2つ目ここまで
            }

            // 引数3つ目の処理
            if (args.length == 3) {

                if(args[0].equalsIgnoreCase("set")){

                    if(args[1].equalsIgnoreCase("main2res")
                            || args[1].equalsIgnoreCase("res2main")){

                        argList.add("n");
                        argList.add("s");
                        argList.add("e");
                        argList.add("w");

                    }

                }

                return argList.stream().filter(a -> a.startsWith(args[2])).collect(Collectors.toList());
            // 3つ目ここまで
            }


        // vivaportal ここまで
        }

        return null;
    // コマンドのTab補完ここまで
    }




// こまごましたやつ

    // コマンド制御用ハッシュマップを作成
    public HashMap<String, Boolean> SelectCommand = new HashMap<String, Boolean>();
    // 選択されたブロック用のハッシュマップ
    public HashMap<String,Location> SelectedBlock = new HashMap<String, Location>();

    // チャンクの距離を計算
    public int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }


    // ポータル名からロケーションを取得
    public Location Portal2Location(String portal_name){

        String world_name = config.getString(portal_name + ".world_name","null");
        double x = config.getDouble(portal_name + ".x",0) + 0.5;
        double y = config.getDouble(portal_name + ".y",65) + 1;
        double z = config.getDouble(portal_name + ".z",0) + 0.5;
        float  d = config.getInt(portal_name + ".direction",0);

        return new Location(getServer().getWorld(world_name),x,y,z,d,0);
    }


    // ゲートウェイの構造を生成
    public void CreateGateway(Location location){

        // ポータルの場所が読み込まれてない場合、終了
        if(!location.isWorldLoaded()) return;

        // ポータルがあるワールドを取得
        World w = location.getWorld();

        // 足元の岩盤の座標を取得
        int x = location.getBlockX();
        int y = location.getBlockY() - 1;
        int z = location.getBlockZ();

        // 足元の岩盤にするブロックから上に向かって生成
        w.getBlockState(x,y,z).getBlock().setType(Material.BEDROCK);
        w.getBlockState(x,y+1,z).getBlock().setType(Material.END_GATEWAY);
        w.getBlockState(x,y+2,z).getBlock().setType(Material.END_GATEWAY);
        w.getBlockState(x,y+3,z).getBlock().setType(Material.END_GATEWAY);
        w.getBlockState(x,y+4,z).getBlock().setType(Material.END_GATEWAY);
        w.getBlockState(x,y+5,z).getBlock().setType(Material.BEDROCK);

        // 周りに空間があるかの確認
        if(!w.getBlockState(x+1,y+1,z).getType().isSolid()
                && !w.getBlockState(x+1,y+2,z).getType().isSolid()) return;
        if(!w.getBlockState(x-1,y+1,z).getType().isSolid()
                && !w.getBlockState(x-1,y+2,z).getType().isSolid()) return;
        if(!w.getBlockState(x,y+1,z+1).getType().isSolid()
                && !w.getBlockState(x,y+2,z+1).getType().isSolid()) return;
        if(!w.getBlockState(x,y+1,z-1).getType().isSolid()
                && !w.getBlockState(x,y+2,z-1).getType().isSolid()) return;
        if(!w.getBlockState(x+1,y+3,z).getType().isSolid()
                && !w.getBlockState(x+1,y+2,z).getType().isSolid()) return;
        if(!w.getBlockState(x-1,y+3,z).getType().isSolid()
                && !w.getBlockState(x-1,y+2,z).getType().isSolid()) return;
        if(!w.getBlockState(x,y+3,z+1).getType().isSolid()
                && !w.getBlockState(x,y+2,z+1).getType().isSolid()) return;
        if(!w.getBlockState(x,y+3,z-1).getType().isSolid()
                && !w.getBlockState(x,y+2,z-1).getType().isSolid()) return;

        // 周りに空間がない場合、AIRを生成
        w.getBlockState(x,y+1,z + 1).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+2,z + 1).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+3,z + 1).getBlock().setType(Material.AIR);
        w.getBlockState(x+1,y+1,z + 1).getBlock().setType(Material.AIR);
        w.getBlockState(x+1,y+2,z + 1).getBlock().setType(Material.AIR);
        w.getBlockState(x+1,y+3,z + 1).getBlock().setType(Material.AIR);

    }

    // ゲートウェイ構造の削除
    public void RemoveGateway(Location location){

        // ポータルの場所が読み込まれてない場合、終了
        if(!location.isWorldLoaded()) return;

        // ポータルがあるワールドを取得
        World w = location.getWorld();

        // 足元の岩盤の座標を取得
        int x = location.getBlockX();
        int y = location.getBlockY() - 1;
        int z = location.getBlockZ();

        // 構造が存在していない場合は、終了
        if(!(w.getBlockState(x,y,z).getBlock().getType() == Material.BEDROCK)) return;
        if(!(w.getBlockState(x,y+5,z).getBlock().getType() == Material.BEDROCK)) return;


        // 足元の岩盤にするブロックから上に向かって生成
        w.getBlockState(x,y,z).getBlock().setType(Material.STONE);
        w.getBlockState(x,y+1,z).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+2,z).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+3,z).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+4,z).getBlock().setType(Material.AIR);
        w.getBlockState(x,y+5,z).getBlock().setType(Material.AIR);

    }















}






