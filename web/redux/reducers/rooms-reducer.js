import * as socketTypes from '../constants/socket-constants'
import Immutable from 'immutable'


export default function rooms(state=Immutable.Map(), action) {
  switch (action.type) {
    case (socketTypes.SOCK_MESSAGE):
    case (socketTypes.SOCK_MEMBERS): {
      return state.set(action.message.room.name, Immutable.fromJS(action.message.room));
    }
    case (socketTypes.SOCK_ROOM_LIST): {
      var nextState = state;
      for (var room of action.message.rooms) {
        nextState = nextState.set(room.name, Immutable.fromJS(room));
      }
      return nextState;
    }
    case (socketTypes.SOCK_UPDATE_ROOM): {
      return state.set(action.message.room.name, Immutable.fromJS(action.message.room));
    }
    default:
      return state
  }
}
