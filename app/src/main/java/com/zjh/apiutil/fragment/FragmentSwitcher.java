package com.zjh.apiutil.fragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.zjh.apiutil.R;

import java.util.Objects;

public class FragmentSwitcher {
    public static final String LoliconAPIFragment = "LoliconAPIFragment";
    public static final String SyxzAPIFragment = "SyxzAPIFragment";
    public static final String TestAPIFragment = "TestAPIFragment";
    public static final String DownloadFragment = "DownloadFragment";
    public static final String GetPixivPicFragment = "GetPixivPicFragment";
    public static final String PixivHotPicFragment = "PixivHotPicFragment";

    private final String [] fragment_tags = new String[]{
            LoliconAPIFragment,
            SyxzAPIFragment,
            TestAPIFragment,
            DownloadFragment,
            GetPixivPicFragment,
            PixivHotPicFragment
    };

    private final Fragment[] fragments = new Fragment[]{
            new LoliconAPIFragment(),
            new SyxzAPIFragment(),
            new TestAPIFragment(),
            new DownloadFragment(),
            new GetPixivPicFragment(),
            new PixivHotPicFragment()
    };

    public String curFragmentTag = null;
    private final FragmentManager fragmentManager;

    public FragmentSwitcher(FragmentManager mgr){
        fragmentManager = mgr;
        add_fragments_with_hide();
    }

    public Fragment find_fragment_by_tag(String tag){
        return Objects.requireNonNull(fragmentManager.findFragmentByTag(tag));
    }

    public void show_fragment_by_tag(String tag){
        show_fragment_by_tag(tag,null);
    }
    public void show_fragment_by_tag(String tag,Runnable run_after_hide){
        if (tag.equals(curFragmentTag)){
            return;
        }
        //hide没有用动画
        //参见fragment_remove.xml的注释
        if(curFragmentTag!=null){
            if(run_after_hide!=null){
                fragmentManager.beginTransaction()
                        .hide(Objects.requireNonNull(fragmentManager.findFragmentByTag(curFragmentTag)))
                        .runOnCommit(run_after_hide).commit();
            }
            else {
                fragmentManager.beginTransaction()
                        .hide(Objects.requireNonNull(fragmentManager.findFragmentByTag(curFragmentTag))).commit();
            }
        }

        curFragmentTag = tag;
        fragmentManager.beginTransaction().setCustomAnimations(R.anim.fragment_slide_in_bottom, R.anim.fragment_remove)
                .show(Objects.requireNonNull(fragmentManager.findFragmentByTag(curFragmentTag))).commit();
    }

    public void hide_curr(){
        if(curFragmentTag==null)
            return;
        fragmentManager.beginTransaction().setCustomAnimations(R.anim.fragment_slide_in_bottom, R.anim.fragment_remove)
                    .hide(Objects.requireNonNull(fragmentManager.findFragmentByTag(curFragmentTag))).commit();
        curFragmentTag=null;
    }

    private void add_fragments_with_hide(){
        for (int i = 0; i < fragments.length; i++){
            fragmentManager.beginTransaction().add(R.id.main_fragment_FrameLayout, fragments[i], fragment_tags[i]).commit();
            fragmentManager.beginTransaction().hide(fragments[i]).commit();
        }
    }

    public void remove_all(){
        for (Fragment fragment : fragments) {
            fragmentManager.beginTransaction().remove(fragment).commit();
        }
    }
}