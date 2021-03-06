import * as types from "../types";
import { make } from "vuex-pathify";
import axios from "axios";
import * as constants from "../../constants/constants";

const state = () => ({
  sondage: {},
  sondageList: []
});

const getters = {
  ...make.getters(state),
  sondages: state => {
    return state.sondageList;
  }
};

const mutations = make.mutations(state);

const actions = {
  ...make.actions(state),
  async [types.sendSondage]({ dispatch, rootState }, sondage) {
    axios.defaults.headers.post['user_key'] = rootState.user.user.token;
    axios.post(constants.API_URL+"api/sondage/add", {
      "question": sondage.question,
      "dateFin": sondage.dateFin,
      "reponsesSondage": sondage.reponseSondages,
      "groupeId": rootState.groupe.groupe.id,
      "userId": rootState.user.user.id,
      "votesAnonymes": sondage.votesAnonymes
    }).then(function (){
      dispatch((`chat/${types.getLiveMessages}`), null, { root: true })
    })
  },
  async [types.getSondage]({ state, rootState}, sondageId){
    axios.defaults.headers.get['user_key'] = rootState.user.user.token;
    axios.get(`${constants.API_URL}api/sondage/${sondageId}`)
      .then(function (response) {
        state.sondageList = [
            ...state.sondageList.filter(sondage => sondage.id !== sondageId),
          response.data
        ];
      })
  },
  async [types.sendVote]({ dispatch, rootState }, payload) {
    axios.defaults.headers.post['user_key'] = rootState.user.user.token;
    axios.post(`${constants.API_URL}api/sondage/${payload.sondageId}/reponse/${payload.reponseId}/vote`, {
      "userId": rootState.user.user.id
    })
      .then(function (){
        dispatch((types.getSondage), payload.sondageId)
    })
  },
};

export const sondage = {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
};
