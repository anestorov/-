package com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import com.oldbg.bulgariankeyboard.CandidateView;
import com.oldbg.bulgariankeyboard.R;

/**
 * Created by rado_sz on 12.06.16.
 */
public class CandidateViewColorChanger {

    //Color theme for corrected suggestions
    private final String CANDIDATE_NORMAL = "#FFD18131";
    private final String CANDIDATE_RECOMMENDED ="#FFD18131";
    private final String CANDIDATE_OTHER ="#FFFFD2A5";

    private CandidateView candidateView;
    private static final CandidateViewColorChanger candidateViewColorChanger = new CandidateViewColorChanger();

    private CandidateViewColorChanger() {
    }

    private CandidateView getCandidateView() {
        return candidateView;
    }

    private void setCandidateView(CandidateView candidateView) {
        this.candidateView = candidateView;
    }

    public static CandidateViewColorChanger getInstance(CandidateView candidateView){
        candidateViewColorChanger.setCandidateView(candidateView);
        return candidateViewColorChanger;
    }

    public void changeColor(int candidateNormal,int candidateRecommed,int candidateOther){
        this.candidateView.setmColorNormal(candidateNormal);
        this.candidateView.setmColorRecommended(candidateRecommed);
        this.candidateView.setmColorOther(candidateOther);
    }
    public void changeColorTheme(){
        int candidateNormal = Color.parseColor(CANDIDATE_NORMAL);
        int candidateRecommended = Color.parseColor(CANDIDATE_RECOMMENDED);
        int candidateOther = Color.parseColor(CANDIDATE_OTHER);
        changeColor(candidateNormal,candidateRecommended,candidateOther);
    }

    public void returnDefaultTheme(Context context){
        int candidateNormal = ContextCompat.getColor(context, R.color.candidate_normal);
        int candidateRecommended= ContextCompat.getColor(context,R.color.candidate_recommended);
        int candidateOther = ContextCompat.getColor(context,R.color.candidate_other);
        changeColor(candidateNormal,candidateRecommended,candidateOther);
    }

    public void changeColor(int color){
        changeColor(color,color,color);
    }
}
