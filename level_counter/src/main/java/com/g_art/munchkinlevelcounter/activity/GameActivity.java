package com.g_art.munchkinlevelcounter.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.g_art.munchkinlevelcounter.R;
import com.g_art.munchkinlevelcounter.analytics.Analytics;
import com.g_art.munchkinlevelcounter.analytics.AnalyticsActions;
import com.g_art.munchkinlevelcounter.listadapter.InGamePlayersAdapter;
import com.g_art.munchkinlevelcounter.listadapter.helper.ItemClickSupport;
import com.g_art.munchkinlevelcounter.model.Player;
import com.g_art.munchkinlevelcounter.model.Sex;
import com.g_art.munchkinlevelcounter.util.SavePlayersStatsTask;
import com.g_art.munchkinlevelcounter.util.SettingsHandler;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by G_Art on 28/7/2014.
 */
public class GameActivity extends AppCompatActivity {

	public static final String MAX_LVL = "max_lvl";
	public static final String PLAYER = "player";
	public static final String PLAYERS_LIST_KEY = "playersList";
	public static final int BATTLE_RESULT_OK = 1;
	public static final int BATTLE_RESULT_CANCEL = 0;
	public static final int BATTLE_RESULT_FAIL = 4;
	public static final int RUN_AWAY_RESULT_OK = 2;
	public static final int RUN_AWAY_RESULT_FAIL = 3;
	public static final int REQUEST_ADD_PLAYER = 11;
	public static final int ADD_PLAYER_RESULT_OK = 1;
	public static final int ADD_PLAYER_RESULT_CANCEL = 0;
	public static final String REQUEST_CODE = "requestCode";
	private static final String BUNDLE_STATS_KEY = "bundleStats";
	private static final int REQUEST_BATTLE = 10;
	private static final String SELECTED_KEY = "selectedPlayer";
	@BindView(R.id.rv_in_game_players)
	RecyclerView mRecyclerView;
	@BindView(R.id.currentPlayer)
	TextView txtCurrentPlayerName;
	@BindView(R.id.txtPlayerLvl)
	TextView txtCurrentPlayerLvl;
	@BindView(R.id.txtPlayerGear)
	TextView txtCurrentPlayerGear;
	@BindView(R.id.total)
	TextView txtCurrentPlayerPower;
	@BindView(R.id.player_sex)
	ImageButton btnSexType;
	@BindView(R.id.btnNextPlayer)
	Button btnNextPlayer;

	private Unbinder unbinder;
	private InGamePlayersAdapter inGameAdapter;
	private ArrayList<Player> playersList;
	private int mSelectedPlayerPosition;
	private Player mSelectedPlayer;
	private boolean contAfterMaxLVL;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		final boolean screenOn = sp.getBoolean(PreferenceScreen.KEY_PREF_SCREEN_ON, false);
		if (screenOn) {
			getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		setContentView(R.layout.activity_game);
		unbinder = ButterKnife.bind(this);

		if (savedInstanceState != null) {
			playersList = savedInstanceState.getParcelableArrayList(PLAYERS_LIST_KEY);
			mSelectedPlayerPosition = savedInstanceState.getInt(SELECTED_KEY);
		} else {
			final Intent intent = getIntent();
			playersList = intent.getParcelableArrayListExtra(PLAYERS_LIST_KEY);
			/*
			Saving first players stats
         	*/
			savePlayersStats();

        	/*
			Setting the first player chosen
         	*/
			mSelectedPlayerPosition = 0;
		}

		intiGameAdapter();
		initRecyclerView();

		// Obtain the shared Tracker instance.
		Analytics.getInstance().logEvent(AnalyticsActions.Open, "GameActivity");

		onPlayerSelected(mSelectedPlayerPosition);
	}

	private void intiGameAdapter() {
		inGameAdapter = new InGamePlayersAdapter(playersList);
	}

	private void initRecyclerView() {
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setAdapter(inGameAdapter);

		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		final ItemClickSupport itemClick = ItemClickSupport.addTo(mRecyclerView);
		itemClick.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
			@Override
			public void onItemClicked(RecyclerView recyclerView, int position, View v) {
				onPlayerSelected(position);
				selectPlayer(position);
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(PLAYERS_LIST_KEY, playersList);
		outState.putInt(SELECTED_KEY, mSelectedPlayerPosition);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.in_game, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@OnClick({
			R.id.btnLvlUp,
			R.id.btnLvlDwn,
			R.id.btnGearUp,
			R.id.btnGearDwn,
			R.id.btnNextPlayer,
			R.id.player_sex
	})
	public void clickPerformed(View v) {
		switch (v.getId()) {
			case R.id.btnLvlUp:
				//noinspection PointlessBooleanExpression
				if (isMaxLvlReached(mSelectedPlayer.getLevel()) == false) {
					mSelectedPlayer.incrementLvl();
				} else {
					if (contAfterMaxLVL) {
						mSelectedPlayer.incrementLvl();
					} else {
						showContinueDialog();
					}
				}
				break;

			case R.id.btnLvlDwn:
				if (mSelectedPlayer.getLevel() != 1) {
					mSelectedPlayer.decrementLvl();
				}
				break;

			case R.id.btnGearUp:
				mSelectedPlayer.incrementGear();
				break;

			case R.id.btnGearDwn:
				mSelectedPlayer.decrementGear();
				break;

			case R.id.btnNextPlayer:
				//noinspection PointlessBooleanExpression
				if (onNextTurnClick(mSelectedPlayer) == false) {
					Toast.makeText(this, getString(R.string.error_next_turn), Toast.LENGTH_LONG).show();
				}
				savePlayersStats();
				break;
			case R.id.player_sex:
				mSelectedPlayer.toggleGender();
				updatePlayerSex();
				break;
		}
		updateSelectedPlayer();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_dice:
				rollADice();
				return true;
			case R.id.action_start_battle:
				showBattle();
				return true;
			case R.id.action_finish:
				savePlayersStats();
				finishGame();
				return true;
			case R.id.action_settings:
				showMaxLvLDialog();
				return true;
			case R.id.action_add_player:
				addPlayerToGame();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void addPlayerToGame() {
		final Intent intent = new Intent(this, PlayerDetailsScreen.class);
		intent.putExtra(REQUEST_CODE, REQUEST_ADD_PLAYER);
		startActivityForResult(intent, REQUEST_ADD_PLAYER);
	}

	@Override
	public void onBackPressed() {
		new MaterialDialog.Builder(this)
				.title(R.string.title_dialog_confirm)
				.content(R.string.message_for_dialog_confirm)
				.positiveText(R.string.ok_btn_for_dialog_confirm)
				.negativeText(R.string.cancel_btn_for_dialog_confirm)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						stayInTheGame();
						dialog.dismiss();
					}
				})
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						leaveGame();
					}
				})
				.autoDismiss(true)
				.show();
	}

	private void leaveGame() {
		Analytics.getInstance().logEvent(AnalyticsActions.Cancel_Game, "GameActivity");

		super.onBackPressed();
	}

	@SuppressWarnings("EmptyMethod")
	private void stayInTheGame() {
	}

	private void onPlayerSelected(int position) {
		mSelectedPlayer = playersList.get(position);
		mSelectedPlayerPosition = position;
		selectPlayer(position);
		updateSelectedPlayer();
		savePlayersStats();
	}

	private void updateSelectedPlayer() {
		if (mSelectedPlayer != null) {
			txtCurrentPlayerName.setText(mSelectedPlayer.getName());
			txtCurrentPlayerLvl.setText(String.valueOf(mSelectedPlayer.getLevel()));
			txtCurrentPlayerGear.setText(String.valueOf(mSelectedPlayer.getGear()));
			txtCurrentPlayerPower.setText(String.valueOf(mSelectedPlayer.getPower()));
			updatePlayerSex();
			updatePlayer(mSelectedPlayerPosition);

			if (playersList.size() == 1) {
				btnNextPlayer.setVisibility(View.GONE);
			} else {
				btnNextPlayer.setVisibility(View.VISIBLE);
			}
		}
	}

	private void updatePlayerSex() {
		if (Sex.MAN == mSelectedPlayer.getSex()) {
			btnSexType.setImageResource(R.drawable.ic_gender_man);
		} else {
			btnSexType.setImageResource(R.drawable.ic_gender_woman);
		}
	}

	private void finishClick() {
		savePlayersStats();
		openStatsActivity();
	}

	private void updatePlayer(int position) {
		inGameAdapter.notifyItemChanged(position);
	}

	private boolean onNextTurnClick(Player player) {
		boolean result;
		try {
			int i = 0;
			for (Player selectedPlayer : playersList) {
				i++;
				if (selectedPlayer.equals(player)) {
					if (i == playersList.size()) {
						i = 0;
					}
					onPlayerSelected(i);
				}
			}

			result = true;
		} catch (Exception ex) {
			result = false;
			Analytics.getInstance().logEvent(AnalyticsActions.Error, "Game_Activity", ex.toString());
		}
		return result;
	}

	private int maxLvl() {
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		return Integer.valueOf(sp.getString(PreferenceScreen.KEY_PREF_MAX_LEVEL, PreferenceScreen.DEFAULT_MAX_LVL_STRING));
	}

	@SuppressWarnings("unchecked")
	private void savePlayersStats() {
		boolean result;
		final SavePlayersStatsTask saveTask = new SavePlayersStatsTask();

		try {
			saveTask.execute(playersList);
			result = saveTask.get();
		} catch (Exception ex) {
			result = false;
		}
	}

	private void rollADice() {
		Analytics.getInstance().logEvent(AnalyticsActions.Dice_Rolled, "GameActivity");
		int Min = 1;
		int Max = 6;
		int dice = Min + (int) (Math.random() * ((Max - Min) + 1));
		final View v = View.inflate(this, R.layout.dice_dialog, null);
		final ImageView imageView = (ImageView) v.findViewById(R.id.imgDice);
		final TextView txtResult = (TextView) v.findViewById(R.id.run_away_result);
		txtResult.setVisibility(View.GONE);

		switch (dice) {
			case 1:
				imageView.setImageResource(R.drawable.ic_dice_1);
				break;
			case 2:
				imageView.setImageResource(R.drawable.ic_dice_2);
				break;
			case 3:
				imageView.setImageResource(R.drawable.ic_dice_3);
				break;
			case 4:
				imageView.setImageResource(R.drawable.ic_dice_4);
				break;
			case 5:
				imageView.setImageResource(R.drawable.ic_dice_5);
				break;
			case 6:
				imageView.setImageResource(R.drawable.ic_dice_6);
				break;
		}

		new MaterialDialog.Builder(this)
				.customView(v, false)
				.title(R.string.dice)
				.autoDismiss(true)
				.show();
	}

	private void showBattle() {
		final Intent intent = new Intent(this, BattleActivity.class);
		final Bundle bundle = new Bundle();
		bundle.putInt(PLAYER, mSelectedPlayerPosition);
		bundle.putParcelableArrayList(PLAYERS_LIST_KEY, playersList);
		bundle.putInt(MAX_LVL, maxLvl());
		intent.putExtras(bundle);

		Analytics.getInstance().logEvent(AnalyticsActions.Battle_Started, "GameActivity");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			View view = findViewById(R.id.action_dice);
			int x = (int) view.getX();
			int y = (int) view.getY();
			final ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeScaleUpAnimation(view, x, y, 0, 0);
			startActivityForResult(intent, REQUEST_BATTLE, optionsCompat.toBundle());
		} else {
			startActivityForResult(intent, REQUEST_BATTLE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_BATTLE == requestCode) {
			handleBattleResult(resultCode);
		} else if (REQUEST_ADD_PLAYER == requestCode) {
			handleNewPlayerResult(resultCode, data);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void handleNewPlayerResult(int requestCode, Intent data) {
		switch (requestCode) {
			case ADD_PLAYER_RESULT_OK:
				if (data != null) {
					final Player addedPlayer = data.getParcelableExtra(PLAYER);
					if (addedPlayer != null) {
						addPlayerToGame(addedPlayer);
					}
				}
				break;
			case ADD_PLAYER_RESULT_CANCEL:
				//do nothing
				break;
			default:
				break;
		}
	}

	private void addPlayerToGame(Player addedPlayer) {
		playersList.add(addedPlayer);
		inGameAdapter.notifyItemInserted(playersList.size() - 1);
		updateSelectedPlayer();
	}

	private void handleBattleResult(int resultCode) {
		switch (resultCode) {
			case BATTLE_RESULT_CANCEL:
				break;
			case BATTLE_RESULT_FAIL:
				break;
			case BATTLE_RESULT_OK:
				break;
			case RUN_AWAY_RESULT_FAIL:
				break;
			case RUN_AWAY_RESULT_OK:
				break;
		}
	}

	private void showContinueDialog() {
		Analytics.getInstance().logEvent(AnalyticsActions.Win_Game, "Game_Activity");
		new MaterialDialog.Builder(this)
				.title(R.string.title_dialog_continue)
				.content(R.string.message_for_dialog_cont)
				.positiveText(R.string.ok_btn_for_dialog_cont)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						doPositiveClickContinueDialog();
					}
				})
				.negativeText(R.string.cancel_btn_for_dialog_cont)
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						doNegativeClickContinueDialog();
					}
				})
				.show();
	}

	private void doPositiveClickContinueDialog() {
		contAfterMaxLVL = true;
		mSelectedPlayer.setWinner(true);
		clickPerformed(findViewById(R.id.btnLvlUp));
	}

	private void doNegativeClickContinueDialog() {
		mSelectedPlayer.setWinner(true);
		mSelectedPlayer.incrementLvl();
		finishClick();
	}

	private void finishGame() {
		Analytics.getInstance().logEvent(AnalyticsActions.Finish_Game, "Game_Activity");
		new MaterialDialog.Builder(this)
				.title(R.string.title_dialog_finish)
				.content(R.string.msg_finish_game)
				.positiveText(R.string.ok_btn_for_dialog_finish)
				.negativeText(R.string.cancel_btn_for_dialog_finish)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						finishCurrentGame();
					}
				})
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						contCurrentGame();
					}
				})
				.autoDismiss(true)
				.show();
	}

	private void finishCurrentGame() {
		openStatsActivity();
	}

	@SuppressWarnings("EmptyMethod")
	private void contCurrentGame() {
	}

	private boolean isMaxLvlReached(int currentLvl) {
		boolean maxLvlReached = false;
		if (currentLvl + 1 == maxLvl()) {
			maxLvlReached = true;
		}
		return maxLvlReached;
	}

	private void openStatsActivity() {
		final Intent intent = new Intent(this, Stats.class);
		final Bundle bundle = new Bundle();
		bundle.putParcelableArrayList(PLAYERS_LIST_KEY, getPlayersList());
		intent.putExtra(BUNDLE_STATS_KEY, bundle);
		startActivity(intent);
	}

	private ArrayList<Player> getPlayersList() {
		return playersList;
	}

	private void showMaxLvLDialog() {
		final MaterialDialog maxLvlDialog = new MaterialDialog.Builder(this)
				.title(R.string.txt_max_lvl)
				.customView(R.layout.maxlvl_dialog, false)
				.positiveText(R.string.dialog_ok_btn)
				.negativeText(R.string.dialog_cancel_btn)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						if (dialog.getCustomView() == null) {
							return;
						}
						final EditText editLvl = (EditText) dialog.getCustomView().findViewById(R.id.maxLvL);
						String lvl = editLvl.getText().toString();
						if (!lvl.equals("") && !lvl.equals(" ")) {
							try {
								int mLvl = Integer.parseInt(lvl);
								if (mLvl <= SettingsHandler.MIN_LVL) {
									Toast.makeText(dialog.getContext(),
											getString(R.string.error_max_level_one),
											Toast.LENGTH_SHORT
									).show();
									editLvl.setText(String.valueOf(maxLvl()), TextView.BufferType.EDITABLE);
								} else {
									doPositiveClickLvLDialog(mLvl);
								}
							} catch (NumberFormatException ex) {
								Toast.makeText(dialog.getContext(),
										getString(R.string.error_max_lvl_settings),
										Toast.LENGTH_SHORT
								).show();
							}
						}
					}
				})
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						contCurrentGame();
					}
				})
				.build();

		if (maxLvlDialog.getCustomView() == null) {
			return;
		}
		final EditText maxLvlEditText = (EditText) maxLvlDialog.getCustomView().findViewById(R.id.maxLvL);
		maxLvlEditText.setText(String.valueOf(maxLvl()), TextView.BufferType.EDITABLE);
		final int position = maxLvlEditText.length();
		maxLvlEditText.setSelection(position);
		maxLvlEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				try {
					if (s.length() > 0 && 1 < Integer.valueOf(s.toString())) {
						maxLvlDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
					} else {
						maxLvlDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
					}
				} catch (Exception e) {
					maxLvlDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
		maxLvlDialog.show();
	}

	private void doPositiveClickLvLDialog(int newMaxLvl) {
		updateMaxLVL(newMaxLvl);
	}

	private void updateMaxLVL(int newMaxLVL) {
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		sp.edit().putString(PreferenceScreen.KEY_PREF_MAX_LEVEL, Integer.toString(newMaxLVL)).apply();
	}

	private void selectPlayer(int position) {
		inGameAdapter.clearSelection();
		inGameAdapter.toggleSelection(position);
		scrollToPlayer(position);
	}

	private void scrollToPlayer(int position) {
		mRecyclerView.scrollToPosition(position);
	}

	@Override
	protected void onDestroy() {
		unbinder.unbind();
		getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onDestroy();
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}
}
