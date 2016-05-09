import * as socketTypes from '../constants/socket-constants'
import Immutable from 'immutable'


export default function messages(state=Immutable.Map(), action) {
  switch (action.type) {
    case (socketTypes.SOCK_SERVER): {
      let message = Immutable.fromJS(action.message);

      return state.set(action.message.room.name, state.get(action.message.room.name, Immutable.List()).push(message));
    }
    case (socketTypes.SOCK_MESSAGE): {
      let message = Immutable.fromJS(action.message.message);

      return state.set(action.message.room.name, state.get(action.message.room.name, Immutable.List()).push(message));
    }
    default:
      return state
  }
}
